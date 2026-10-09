import io
import unittest
from contextlib import redirect_stderr
from datetime import date

import sync_project
from test_validate import graph, node


class SyncProjectTest(unittest.TestCase):
    def test_missing_project_scope_exits_2_with_refresh_hint(self):
        def run(*args, repo=None):
            if args[0] == "repo":
                return '{"nameWithOwner": "o/r"}'
            raise RuntimeError("error: your authentication token is missing required scopes [read:project]")

        err = io.StringIO()
        with redirect_stderr(err):
            code = sync_project.main(["--graph", str(sync_project.DEFAULT_PATH)], run=run)
        self.assertEqual(code, 2)
        self.assertIn("gh auth refresh -s project", err.getvalue())

    def test_plan_dates_follow_dependencies_and_recorded_dates(self):
        g = graph(
            node("a", kind="harness", status="done", start="2026-10-01", end="2026-10-02"),
            node("b", ["a"], kind="ui"),
            node("c", ["b"], kind="check"),
            node("d", ["a"], kind="common", status="doing", start="2026-10-03"),
        )
        plan = sync_project.plan_dates(g, date(2026, 10, 9))
        self.assertEqual(plan["a"], (date(2026, 10, 1), date(2026, 10, 2)))
        self.assertEqual(plan["b"], (date(2026, 10, 9), date(2026, 10, 11)))
        self.assertEqual(plan["c"], (date(2026, 10, 12), date(2026, 10, 12)))
        self.assertEqual(plan["d"], (date(2026, 10, 3), date(2026, 10, 9)))


if __name__ == "__main__":
    unittest.main()
