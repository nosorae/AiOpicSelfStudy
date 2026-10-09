import json
import unittest

from validate import loop_limits, validate

LOOP = {
    "steps": ["implement", "done", "review", "merge"],
    "edges": [
        {"from": "implement", "to": "done"},
        {"from": "done", "to": "review", "on": "pass"},
        {"from": "done", "to": "implement", "on": "fail", "max": 3},
        {"from": "review", "to": "merge", "on": "approve"},
        {"from": "review", "to": "implement", "on": "reject", "max": 2},
    ],
    "exceeded": "blocked",
}


def node(id, deps=(), owns=(), **extra):
    n = {"id": id, "phase": "P0", "kind": "common", "title": id, "dependsOn": list(deps),
         "owns": list(owns), "reqs": [], "done": "./gradlew check", "done_desc": "테스트 통과", "status": "todo"}
    n.update(extra)
    return n


def graph(*nodes):
    return {"loop": json.loads(json.dumps(LOOP)), "phases": [{"id": "P0", "title": "준비"}], "nodes": list(nodes)}


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

    def test_done_must_be_command_or_human(self):
        self.assertEqual(validate(graph(node("a", done="human"))), [])
        errors = validate(graph(node("a", done="테스트가 통과한다")))
        self.assertTrue(any("done_desc" in e for e in errors), errors)

    def test_missing_done_desc(self):
        n = node("a")
        del n["done_desc"]
        self.assertTrue(any("done_desc" in e for e in validate(graph(n))))

    def test_loop_template_required(self):
        g = graph(node("a"))
        del g["loop"]
        self.assertTrue(any("loop" in e for e in validate(g)))

    def test_backward_edge_needs_limit(self):
        g = graph(node("a"))
        del g["loop"]["edges"][2]["max"]
        errors = validate(g)
        self.assertTrue(any("max" in e for e in errors), errors)

    def test_inner_loop_is_not_a_dependency_cycle(self):
        # 템플릿에는 done 에서 implement 로 되돌아가는 엣지가 있지만 노드 사이 순환이 아니다
        self.assertEqual(validate(graph(node("a"), node("b", ["a"]))), [])

    def test_node_overrides_limit(self):
        g = graph(node("a", loop={"done": 5}))
        self.assertEqual(validate(g), [])
        self.assertEqual(loop_limits(g, g["nodes"][0]), {"done": 5, "review": 2})

    def test_node_override_and_attempts_must_name_limited_step(self):
        errors = validate(graph(node("a", loop={"merge": 1}), node("b", attempts={"done": -1})))
        self.assertTrue(any("a" in e and "merge" in e for e in errors), errors)
        self.assertTrue(any("b" in e and "attempts" in e for e in errors), errors)


if __name__ == "__main__":
    unittest.main()
