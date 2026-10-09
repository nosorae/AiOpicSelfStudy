import json
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

from test_validate import graph, node

HERE = Path(__file__).resolve().parent


class RunDoneTest(unittest.TestCase):
    """임시 저장소 모양(scripts/graph, docs/graph)에 그래프를 두고 run_done.sh 를 돌린다. set_status.sh 는 호출만 기록한다."""

    def setUp(self):
        self.root = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.root)
        scripts = self.root / "scripts" / "graph"
        scripts.mkdir(parents=True)
        for name in ("validate.py", "run_done.sh"):
            shutil.copy(HERE / name, scripts / name)
        stub = scripts / "set_status.sh"
        stub.write_text('#!/usr/bin/env bash\necho "$@" > "$(dirname "$0")/set_status.called"\n')
        stub.chmod(0o755)
        self.called = scripts / "set_status.called"
        self.path = self.root / "docs" / "graph" / "dev-graph.json"
        self.path.parent.mkdir(parents=True)

    def run_node(self, n, *args):
        self.path.write_text(json.dumps(graph(n), ensure_ascii=False))
        code = subprocess.run(["bash", str(self.root / "scripts/graph/run_done.sh"), n["id"], *args],
                              capture_output=True, text=True).returncode
        return code, json.loads(self.path.read_text())["nodes"][0]

    def test_pass_keeps_attempts(self):
        code, n = self.run_node(node("a", done="true"))
        self.assertEqual(code, 0)
        self.assertNotIn("attempts", n)

    def test_human_exits_2(self):
        self.assertEqual(self.run_node(node("a", done="human"))[0], 2)

    def test_failure_counts_then_blocks_over_limit(self):
        code, n = self.run_node(node("a", done="exit 3", attempts={"done": 2}))
        self.assertEqual((code, n["attempts"]["done"]), (3, 3))
        self.assertFalse(self.called.exists())
        code, n = self.run_node(node("a", done="exit 3", attempts={"done": 3}))
        self.assertEqual((code, n["attempts"]["done"]), (3, 4))
        self.assertTrue(self.called.read_text().startswith("a blocked"))

    def test_review_rejection_uses_node_override(self):
        code, n = self.run_node(node("a", loop={"review": 1}), "review")
        self.assertEqual((code, n["attempts"]["review"]), (0, 1))
        code, _ = self.run_node(node("a", loop={"review": 1}, attempts={"review": 1}), "review")
        self.assertEqual(code, 1)
        self.assertTrue(self.called.exists())


if __name__ == "__main__":
    unittest.main()
