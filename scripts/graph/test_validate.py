import unittest

from validate import validate


def node(id, deps=(), owns=(), **extra):
    n = {"id": id, "phase": "P0", "kind": "common", "title": id, "dependsOn": list(deps),
         "owns": list(owns), "reqs": [], "done": "테스트 통과", "status": "todo"}
    n.update(extra)
    return n


def graph(*nodes):
    return {"phases": [{"id": "P0", "title": "준비"}], "nodes": list(nodes)}


class ValidateTest(unittest.TestCase):
    def test_valid_graph_has_no_errors(self):
        g = graph(node("a", owns=["libs/a/"]), node("b", ["a"], ["libs/a/sub/"]), node("c", ["a"], ["libs/c/"]))
        self.assertEqual(validate(g), [])

    def test_missing_field(self):
        n = node("a")
        del n["done"]
        errors = validate(graph(n))
        self.assertTrue(any("done" in e for e in errors), errors)

    def test_unknown_status_and_phase(self):
        errors = validate(graph(node("a", status="wip", phase="P9")))
        self.assertTrue(any("status" in e for e in errors), errors)
        self.assertTrue(any("P9" in e for e in errors), errors)

    def test_duplicate_id(self):
        self.assertTrue(any("중복" in e for e in validate(graph(node("a"), node("a")))))

    def test_missing_dependency(self):
        errors = validate(graph(node("a", ["ghost"])))
        self.assertTrue(any("ghost" in e for e in errors), errors)

    def test_cycle(self):
        errors = validate(graph(node("a", ["c"]), node("b", ["a"]), node("c", ["b"])))
        self.assertTrue(any("순환" in e for e in errors), errors)

    def test_parallel_nodes_with_overlapping_paths(self):
        g = graph(node("root"), node("x", ["root"], ["engine/answer/"]), node("y", ["root"], ["engine/answer/store/"]))
        errors = validate(g)
        self.assertTrue(any("x" in e and "y" in e for e in errors), errors)

    def test_glob_overlap(self):
        g = graph(node("x", owns=["libs/*/README.md"]), node("y", owns=["libs/tts/"]))
        self.assertEqual(len(validate(g)), 1)

    def test_sequential_nodes_may_share_paths(self):
        g = graph(node("x", owns=["ui/"]), node("y", ["x"], ["ui/feature/"]), node("z", ["y"], ["ui/feature/a/"]))
        self.assertEqual(validate(g), [])

    def test_disjoint_siblings(self):
        g = graph(node("x", owns=["libs/recorder/src/androidMain/"]), node("y", owns=["libs/recorder/src/iosMain/"]))
        self.assertEqual(validate(g), [])

    def test_issue_must_be_number(self):
        self.assertTrue(validate(graph(node("a", issue="#3"))))


if __name__ == "__main__":
    unittest.main()
