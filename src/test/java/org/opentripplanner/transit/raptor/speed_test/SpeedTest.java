package org.opentripplanner.transit.raptor.speed_test;

import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.TransitLayerMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.raptor.RangeRaptorService;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.RangeRaptorRequest;
import org.opentripplanner.transit.raptor.api.transit.TransitDataProvider;
import org.opentripplanner.transit.raptor.speed_test.api.model.TripPlan;
import org.opentripplanner.transit.raptor.speed_test.cli.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.raptor.speed_test.testcase.CsvFileIO;
import org.opentripplanner.transit.raptor.speed_test.testcase.NoResultFound;
import org.opentripplanner.transit.raptor.speed_test.testcase.TestCase;
import org.opentripplanner.transit.raptor.speed_test.transit.EgressAccessRouter;
import org.opentripplanner.transit.raptor.speed_test.transit.ItineraryMapper;
import org.opentripplanner.transit.raptor.speed_test.transit.ItinerarySet;
import org.opentripplanner.transit.raptor.speed_test.transit.TripPlanSupport;
import org.opentripplanner.transit.raptor.util.AvgTimer;

import java.io.File;
import java.lang.ref.WeakReference;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test response times for a large batch of origin/destination points.
 * Also demonstrates how to run basic searches without using the graphQL profile routing API.
 */
public class SpeedTest {

    private static final boolean TEST_NUM_OF_ADDITIONAL_TRANSFERS = false;
    private static final String TRAVEL_SEARCH_FILENAME = "travelSearch";

    private final Graph graph;
    private final TransitLayer transitLayer;


    private final AvgTimer TOT_TIMER = AvgTimer.timerMilliSec("SpeedTest:route");
    private final AvgTimer TIMER_WORKER = AvgTimer.timerMilliSec("SpeedTest:route Worker");
    private final AvgTimer TIMER_COLLECT_RESULTS = AvgTimer.timerMilliSec("SpeedTest: Collect Results");

    private final SpeedTestCmdLineOpts opts;
    private int nAdditionalTransfers;
    private SpeedTestProfile routeProfile;
    private SpeedTestProfile heuristicProfile;
    private final EgressAccessRouter streetRouter;
    private List<Integer> numOfPathsFound = new ArrayList<>();
    private Map<SpeedTestProfile, List<Integer>> workerResults = new HashMap<>();
    private Map<SpeedTestProfile, List<Integer>> totalResults = new HashMap<>();

    /**
     * Init profile used by the HttpServer
     */
    private RangeRaptorService<TripSchedule> service;


    private SpeedTest(SpeedTestCmdLineOpts opts) {
        this.opts = opts;
        this.graph = loadGraph(opts.rootDir());
        this.transitLayer = TransitLayerMapper.map(graph);
        this.streetRouter = new EgressAccessRouter(graph, transitLayer);
        this.nAdditionalTransfers = opts.numOfExtraTransfers();

        // Init Raptor Service
        this.service = new RangeRaptorService<>(SpeedTestRequest.TUNING_PARAMETERS);
    }

    public static void main(String[] args) throws Exception {
        // Given the following setup
        AvgTimer.enableTimers(true);
        SpeedTestCmdLineOpts opts = new SpeedTestCmdLineOpts(args);

        // create a new test
        SpeedTest speedTest = new SpeedTest(opts);

        // and run it
        speedTest.runTest();
    }

    private static Graph loadGraph(File rootDir) {
        Graph graph = Graph.load(OtpDataStore.graphFile(rootDir));
        graph.index();
        return graph;
    }

    private void runTest() throws Exception {
        final SpeedTestProfile[] speedTestProfiles = opts.profiles();
        final int nSamples = opts.numberOfTestsSamplesToRun();

        initProfileStatistics();

        for (int i = 0; i < nSamples; ++i) {
            setupSingleTest(speedTestProfiles, i, nSamples);
            runSingleTest(i+1, nSamples);
        }
        printProfileStatistics();

        service.shutdown();
    }

    private void runSingleTest(int sample, int nSamples) throws Exception {
        CsvFileIO tcIO = new CsvFileIO(opts.rootDir(), TRAVEL_SEARCH_FILENAME);
        List<TestCase> testCases = tcIO.readTestCasesFromFile();
        List<TripPlan> tripPlans = new ArrayList<>();


        int nSuccess = 0;
        numOfPathsFound.clear();

        // Force GC to avoid GC during the test
        forceGCToAvoidGCLater();

        List<String> testCaseIds = opts.testCases();
        List<TestCase> testCasesToRun;

        if(testCaseIds.isEmpty()) {
            testCasesToRun = testCases;
        }
        else {
            testCasesToRun = testCases.stream().filter(it -> testCaseIds.contains(it.id)).collect(Collectors.toList());
        }

        // Warm up JIT compiler
        runSingleTestCase(tripPlans, testCases.get(1), true);

        ResultPrinter.logSingleTestHeader(routeProfile);
        AvgTimer.resetAll();

        for (TestCase testCase : testCasesToRun) {
            nSuccess += runSingleTestCase(tripPlans, testCase, false) ? 1 : 0;
        }

        int tcSize = testCasesToRun.size();
        workerResults.get(routeProfile).add((int) TIMER_WORKER.avgTime());
        totalResults.get(routeProfile).add((int) TOT_TIMER.avgTime());

        ResultPrinter.logSingleTestResult(
                routeProfile, numOfPathsFound, sample, nSamples, nSuccess, tcSize, TOT_TIMER.totalTimeInSeconds()
        );

        tcIO.writeResultsToFile(testCases);
    }

    private void printProfileStatistics() {
        ResultPrinter.printProfileResults("Worker: ", workerResults);
        ResultPrinter.printProfileResults("Total:  ", totalResults);
    }

    private void initProfileStatistics() {
        for (SpeedTestProfile key : opts.profiles()) {
            workerResults.put(key, new ArrayList<>());
            totalResults.put(key, new ArrayList<>());
        }
    }

    private boolean runSingleTestCase(List<TripPlan> tripPlans, TestCase testCase, boolean ignoreResults) {
        try {
            final SpeedTestRequest request = new SpeedTestRequest(testCase, opts, getTimeZoneId());

            if (opts.compareHeuristics()) {
                TOT_TIMER.start();
                SpeedTestRequest heurReq = new SpeedTestRequest(testCase, opts, getTimeZoneId());
                compareHeuristics(heurReq, request);
                TOT_TIMER.stop();
            } else {
                // Perform routing
                TOT_TIMER.start();
                TripPlan route = route(request, testCase.arrivalTime);
                TOT_TIMER.stop();

                if (!ignoreResults) {
                    tripPlans.add(route);
                    testCase.assertResult(route.getItineraries());
                    ResultPrinter.printResultOk(testCase, TOT_TIMER.lapTime(), opts.verbose());
                }
            }
            return true;
        } catch (Exception e) {
            TOT_TIMER.failIfStarted();
            if (!ignoreResults) {
                ResultPrinter.printResultFailed(testCase, TOT_TIMER.lapTime(), e);
            }
            return false;
        }
    }


    public TripPlan route(SpeedTestRequest request, int latestArrivalTime) {
        try {
            Collection<Path<TripSchedule>> paths;
            streetRouter.route(request);

            // -------------------------------------------------------- [ WORKER ROUTE ]

            TransitDataProvider<TripSchedule> transitData = transitData(request);

            TIMER_WORKER.start();

            RangeRaptorRequest<TripSchedule> req = rangeRaptorRequest(routeProfile, request, latestArrivalTime, streetRouter);

            paths = service.route(req, transitData);


            TIMER_WORKER.stop();

            // -------------------------------------------------------- [ COLLECT RESULTS ]

            TIMER_COLLECT_RESULTS.start();

            if (paths.isEmpty()) {
                numOfPathsFound.add(0);
                throw new NoResultFound();
            }
            numOfPathsFound.add(paths.size());

            TripPlan tripPlan = mapToTripPlan(request, paths, streetRouter);

            TIMER_COLLECT_RESULTS.stop();

            return tripPlan;
        } finally {
            TIMER_WORKER.failIfStarted();
            TIMER_COLLECT_RESULTS.failIfStarted();
        }
    }

    private void compareHeuristics(SpeedTestRequest heurReq, SpeedTestRequest routeReq) {
        streetRouter.route(heurReq);
        TransitDataProvider<TripSchedule> transitData = transitData(heurReq);
        int latestArrivalTime = routeReq.getArrivalTime();

        RangeRaptorRequest<TripSchedule> req1 = heuristicRequest(
                heuristicProfile, heurReq, latestArrivalTime, streetRouter
        );
        RangeRaptorRequest<TripSchedule> req2 = heuristicRequest(
                routeProfile, routeReq, latestArrivalTime, streetRouter
        );

        TIMER_WORKER.start();
        service.compareHeuristics(req1, req2, transitData);
        TIMER_WORKER.stop();
    }

    private void setupSingleTest(
            SpeedTestProfile[] profilesToRun,
            int sample,
            int nSamples
    ) {
        if (opts.compareHeuristics()) {
            heuristicProfile = profilesToRun[0];
            routeProfile = profilesToRun[1 + sample % (profilesToRun.length - 1)];
        } else {
            heuristicProfile = null;
            routeProfile = profilesToRun[sample % profilesToRun.length];
        }

        // Enable flag to test the effect of counting down the number of additional transfers limit
        if (TEST_NUM_OF_ADDITIONAL_TRANSFERS) {
            // sample start at 1 .. nSamples(inclusive)
            nAdditionalTransfers = 1 + ((nSamples-1) - sample) / profilesToRun.length;
            System.err.println("\n>>> Run test with " + nAdditionalTransfers + " number of additional transfers.\n");
        }
    }

    private ZoneId getTimeZoneId() {
        return graph.getTimeZone().toZoneId();
    }

    private RangeRaptorRequest<TripSchedule> heuristicRequest(
            SpeedTestProfile profile,
            SpeedTestRequest request,
            int latestArrivalTime,
            EgressAccessRouter streetRouter
    ) {
        return request.createRangeRaptorRequest(
                profile,
                latestArrivalTime,
                nAdditionalTransfers,
                true,
                streetRouter
        );
    }


    private RangeRaptorRequest<TripSchedule> rangeRaptorRequest(
            SpeedTestProfile profile,
            SpeedTestRequest request,
            int latestArrivalTime,
            EgressAccessRouter streetRouter
    ) {
        return request.createRangeRaptorRequest(
                profile, latestArrivalTime, nAdditionalTransfers, false, streetRouter
        );
    }

    private TripPlan mapToTripPlan(SpeedTestRequest request, Collection<Path<TripSchedule>> paths, EgressAccessRouter streetRouter) {
        ItinerarySet itineraries = ItineraryMapper.mapItineraries(request, paths, streetRouter, transitLayer);

        // Filter away similar itineraries for easier reading
        // itineraries.filter();

        return TripPlanSupport.createTripPlanForRequest(request, itineraries);
    }

    private TransitDataProvider<TripSchedule> transitData(SpeedTestRequest request) {
        return new RaptorRoutingRequestTransitData(
                transitLayer,
                request.getDepartureDateWithZone().toInstant(),
                2,
                request.getTransitModes(),
                request.getWalkSpeedMeterPrSecond()
        );
    }

    private void forceGCToAvoidGCLater() {
        WeakReference ref = new WeakReference<>(new Object());
        while (ref.get() != null) {
            System.gc();
        }
    }
}