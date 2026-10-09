import io
import json
import unittest
from contextlib import redirect_stdout

import sync_issues
from test_validate import graph, node


def fake_gh(issues):
    calls = []

    def run(*args, repo=None):
        calls.append(args)
        if args[:2] == ("label", "list"):
            return "[]"
        if args[:2] == ("issue", "list"):
            return json.dumps(issues)
        return ""

    return run, calls


class SyncIssuesTest(unittest.TestCase):
    def test_title_follows_graph_without_reopening_closed_issue(self):
        g = graph(node("a", title="새 제목", issue=1), node("b", title="같은 제목", issue=2, status="done"))
        run, calls = fake_gh([
            {"number": 1, "state": "CLOSED", "title": "[a] 옛 제목", "labels": []},
            {"number": 2, "state": "CLOSED", "title": "[b] 같은 제목", "labels": []},
        ])
        with redirect_stdout(io.StringIO()):
            sync_issues.sync(g, None, run=run)
        self.assertIn(("issue", "edit", "1", "--title", "[a] 새 제목"), calls)
        self.assertFalse(any(c[:2] == ("issue", "edit") and c[2] == "2" for c in calls), calls)
        self.assertFalse(any(c[:2] == ("issue", "reopen") for c in calls), calls)


if __name__ == "__main__":
    unittest.main()
