"""Offline contracts for the registered paired study; no Android PASS implied."""
import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HELPERS = ROOT / 'scripts/performance'
sys.path.insert(0, str(HELPERS))
SPEC = importlib.util.spec_from_file_location('alignment_study', HELPERS / 'research_study.py')
STUDY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(STUDY)


class ResearchTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.log = self.root / 'logcat-com.solthariel.pawchan.performance.ComposerLayoutBenchmark-warmErrorLayout.txt'
        self.rows = [dict(schema=1, pid=100+p, action=a, measureCalls=1, placeCalls=20, measureNs=1000000)
                     for p in range(5) for a in range(1, 13)]

    def save(self):
        self.log.write_text('\n'.join('I PawResearch: '+STUDY.PREFIX+json.dumps(r) for r in self.rows))

    def reject(self):
        self.save()
        with self.assertRaises(ValueError):
            STUDY.read_actions(self.root)

    def test_warmups_excluded_and_fifty_actions_retained(self):
        self.save()
        rows = STUDY.read_actions(self.root)
        self.assertEqual(50, len(rows))
        self.assertEqual({3,4,5,6,7,8,9,10,11,12}, {r['action'] for r in rows})

    def test_missing_action_rejected(self):
        self.rows.pop(); self.reject()

    def test_duplicate_action_rejected(self):
        self.rows.append(self.rows[-1]); self.reject()

    def test_missing_process_rejected(self):
        self.rows = self.rows[12:]; self.reject()

    def test_malformed_json_rejected(self):
        self.log.write_text(STUDY.PREFIX+'{broken')
        with self.assertRaises(ValueError):
            STUDY.read_actions(self.root)

    def test_boolean_count_rejected(self):
        self.rows[0]['measureCalls'] = True; self.reject()

    def test_negative_duration_rejected(self):
        self.rows[0]['measureNs'] = -1; self.reject()

    def test_zero_calls_with_duration_rejected(self):
        self.rows[0]['measureCalls'] = 0; self.reject()

    def test_zero_calls_and_zero_duration_are_valid(self):
        self.rows[2].update(measureCalls=0, measureNs=0); self.save()
        self.assertEqual(0, STUDY.read_actions(self.root)[0]['measureCalls'])

    def test_missing_logcat_rejected(self):
        with self.assertRaises(ValueError):
            STUDY.read_actions(self.root)

    def test_ambiguous_logcat_rejected(self):
        self.save(); d=self.root/'duplicate'; d.mkdir(); (d/self.log.name).write_text(self.log.read_text())
        with self.assertRaises(ValueError):
            STUDY.read_actions(self.root)

    def test_incomplete_pair_count_rejected(self):
        with self.assertRaises(ValueError):
            STUDY.decide([])

    def test_count_signal_requires_all_four_pairs(self):
        p=dict(callDelta=.2, durationDeltaNs=0, durationRatio=1)
        result=STUDY.decide([p]*4)
        self.assertTrue(result['countSignal']); self.assertFalse(result['performancePass'])
        other=dict(p,callDelta=0)
        self.assertFalse(STUDY.decide([p,p,p,other])['countSignal'])

    def test_duration_signal_requires_relative_and_absolute_thresholds(self):
        p=dict(callDelta=0, durationDeltaNs=600000, durationRatio=1.3)
        self.assertTrue(STUDY.decide([p]*4)['durationSignal'])
        self.assertFalse(STUDY.decide([dict(p,durationDeltaNs=10000)]*4)['durationSignal'])
        self.assertFalse(STUDY.decide([dict(p,durationRatio=1.05)]*4)['durationSignal'])

    def test_no_count_signal_is_not_global_performance_pass(self):
        p=dict(callDelta=0, durationDeltaNs=0, durationRatio=1)
        result=STUDY.decide([p]*4)
        self.assertIn('HYPOTHESIS_NOT_SUPPORTED', result['outcome'])
        self.assertFalse(result['performancePass']); self.assertFalse(result['automaticAdditionalRuns'])

    def samples(self):
        return [dict(batteryTemperatureC=36, thermal=0,hz=60,brightness='219',brightnessMode='0',usb='true') for _ in range(4)]

    def test_identical_conditions_valid(self):
        STUDY.validate_conditions(self.samples())

    def test_hot_device_rejected(self):
        s=self.samples();s[0]['thermal']=2
        with self.assertRaises(ValueError):STUDY.validate_conditions(s)

    def test_temperature_spread_rejected(self):
        s=self.samples();s[0]['batteryTemperatureC']=39
        with self.assertRaises(ValueError):STUDY.validate_conditions(s)

    def test_refresh_or_brightness_change_rejected(self):
        for key,value in [('hz',90),('brightness','220'),('usb','false')]:
            s=self.samples();s[0][key]=value
            with self.assertRaises(ValueError):STUDY.validate_conditions(s)

    def test_plan_is_balanced_and_thresholds_match_implementation(self):
        plan=json.loads((HELPERS/'alignment-study-plan.json').read_text())
        self.assertEqual(STUDY.ORDERS,plan['orders'])
        self.assertEqual(2,plan['orders'].count('CandidateFirst'))
        self.assertEqual(.1,plan['minimumCallDelta'])
        self.assertEqual(.2,plan['secondaryDurationRelativeDelta'])
        self.assertEqual(500000,plan['secondaryDurationAbsoluteNs'])

    def test_opt_in_main_path_and_warmup_counts_unchanged(self):
        source=(ROOT/'feature/composer/src/benchmark/kotlin/com/solthariel/pawchan/composer/ComposerPerformanceActivity.kt').read_text()
        self.assertIn('getBooleanExtra("paw_research", false)',source)
        self.assertIn('if (research) AlignmentResearch.toggle(!expanded)',source)
        driver=(HELPERS/'Measure-ComposerPerformance.ps1').read_text()
        self.assertIn('if ($Research) { $Diagnostics = $true }',driver)
        self.assertIn('NonInteractive is only allowed',driver)


if __name__ == '__main__':unittest.main()
