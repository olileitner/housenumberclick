package org.openstreetmap.josm.plugins.housenumberclick;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Optional deep consistency checks that intentionally rely on source-text assertions.
 * Keep these out of the fast regression target and run them explicitly when needed.
 */
public final class HouseNumberClickDeepConsistencyChecks {

    private HouseNumberClickDeepConsistencyChecks() {
        // Utility class
    }

    public static void main(String[] args) {
        int exitCode = 0;
        try {
            List<DeepCheck> checks = List.of(
                    new DeepCheck("Street fallback click readback initializes house number to one",
                            "testStreetFallbackClickReadbackInitializesHouseNumber"),
                    new DeepCheck("Overwrite warning supports independent street and postcode suppression",
                            "testOverwriteWarningSupportsIndependentStreetAndPostcodeSuppression"),
                    new DeepCheck("Ctrl has priority over Alt split activation", "testCtrlHasPriorityOverAltActivation"),
                    new DeepCheck("Ctrl+Shift click is passed through to JOSM core", "testCtrlShiftClickIsPassedThrough"),
                    new DeepCheck("Temporary Alt split exits on Alt release", "testTemporaryAltSplitExitsOnAltRelease"),
                    new DeepCheck("Alt+digit sets row-house parts through controller", "testAltDigitSetsTerracePartsShortcut"),
                    new DeepCheck("Alt+digit shortcut requires plain Alt", "testAltDigitShortcutRequiresPlainAlt"),
                    new DeepCheck("Primary apply restores map focus for undo shortcuts", "testPrimaryApplyRestoresMapFocusForUndoShortcuts"),
                    new DeepCheck("Overlay self-heal check is wired into interaction flow", "testOverlaySelfHealInteractionHooks"),
                    new DeepCheck("Row-house parts dialog sync avoids document mutation during notifications",
                            "testRowHousePartsDialogSyncDefersDocumentMutation"),
                    new DeepCheck("Ctrl cursor uses custom magnifier without arrow asset fallback", "testCtrlCursorUsesCustomMagnifier"),
                    new DeepCheck("Split cursor hotspot keeps scalp tip shifted left", "testSplitCursorHotspotShiftedLeft"),
                    new DeepCheck("Split map mode is line-split only", "testSplitMapModeIsLineSplitOnly"),
                    new DeepCheck("Reference cache is invalidated on data source changes", "testReferenceCacheInvalidationOnDataSourceChange"),
                    new DeepCheck("Reference async results are generation-guarded across lifecycle", "testReferenceLoadGenerationGuardOnLifecycle"),
                    new DeepCheck("Street selection is re-resolved against current street index", "testStreetSelectionReResolutionOrder"),
                    new DeepCheck("Readback street selection stays spatially disambiguated for same-name streets",
                            "testReadbackStreetSelectionUsesSpatialDisambiguation"),
                    new DeepCheck("Undo queue changes trigger visual rescan refresh", "testUndoQueueChangesTriggerVisualRescanRefresh"),
                    new DeepCheck("Rectangularize option is propagated to temporary line split mode", "testRectangularizePreferencePropagation"),
                    new DeepCheck("House-number cursor label depends on complete address inputs", "testHouseNumberCursorLabelCompletenessGuard"),
                    new DeepCheck("Completeness legend labels are present", "testCompletenessLegendLabelsPresent"),
                    new DeepCheck("Completeness layer is named consistently", "testCompletenessLayerNameConsistency"),
                    new DeepCheck("Analysis section has no text postcode legend", "testAnalysisSectionHasNoTextPostcodeLegend"),
                    new DeepCheck("Overview layer toggles are mutually exclusive", "testOverviewLayerTogglesAreMutuallyExclusive"),
                    new DeepCheck("Street table click syncs main dialog selection", "testStreetTableClickSyncsMainDialogSelection"),
                    new DeepCheck("Street table selection respects AutoZoom option", "testStreetTableSelectionRespectsAutoZoomOption"),
                    new DeepCheck("Closing overview dialogs updates main dialog checkboxes", "testOverviewDialogCloseUpdatesMainDialogCheckboxes"),
                    new DeepCheck("Previous/Next street navigation clears postcode and house number", "testStreetNavigationClearsPostcodeAndHouseNumber"),
                    new DeepCheck("Street combo popup hover does not trigger transient overlay refresh", "testStreetSelectionIgnoresTransientPopupHover"),
                    new DeepCheck("Street auto-zoom uses full-dataset street index", "testStreetAutoZoomUsesFullDataSetStreetIndex"),
                    new DeepCheck("House-number overlay collector ignores relation/outer self-duplicates",
                            "testHouseNumberOverlayCollectorIgnoresRelationOuterSelfDuplicate"),
                    new DeepCheck("House-number overlay collector keeps duplicates across distinct real buildings",
                            "testHouseNumberOverlayCollectorKeepsDistinctBuildingDuplicates"),
                    new DeepCheck("House-number overlay duplicate key remains city-agnostic",
                            "testHouseNumberOverlayDuplicateKeyRemainsCityAgnostic"),
                    new DeepCheck("Terrace split failure path performs rollback", "testTerraceSplitFailurePathRollsBackCommands"),
                    new DeepCheck("Terrace split orientation supports non-rectangular outlines", "testTerraceSplitOrientationSupportsNonRectangularOutlines"),
                    new DeepCheck("Dialog no longer exposes split-start button entrypoints", "testDialogSplitStartEntrypointsRemoved"),
                    new DeepCheck("Controller no longer exposes split mode activation entrypoints", "testSplitModeControllerEntrypointsRemoved")
            );

            for (DeepCheck check : checks) {
                invokeRiskRegressionMethod(check.name, check.methodName);
            }
            System.out.println("All deep consistency checks passed.");
        } catch (Throwable t) {
            exitCode = 1;
            t.printStackTrace(System.err);
        } finally {
            System.exit(exitCode);
        }
    }

    private static void invokeRiskRegressionMethod(String name, String methodName) throws Exception {
        try {
            Method method = HouseNumberClickRiskRegressionTests.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(null);
            System.out.println("[PASS] " + name);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw ex;
        }
    }

    private static final class DeepCheck {
        private final String name;
        private final String methodName;

        private DeepCheck(String name, String methodName) {
            this.name = name;
            this.methodName = methodName;
        }
    }
}


