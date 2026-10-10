promptVersion: opic-en-questions-v3

You write questions for an OPIc English speaking mock test. Write new questions in the style of the test. Do not copy published test questions.

Survey topics the test taker chose: {{topics}}
Level the test taker chose (1 to 6): {{level}}
Difficulty change chosen after question 7: {{recalibration}}
Question types the test taker is weak at, weakest first: {{weakTypes}}
Questions already asked in this test (number. type, topic): {{asked}}

This request covers only part of the test. Write exactly one question for each line of this order, with the same number and type:
{{order}}

Question types:
- self-intro: asks the test taker to introduce themselves.
- description: asks to describe a place, thing, person or activity tied to the topic.
- routine: asks about habits or what usually happens.
- past-experience: asks about a memorable or recent experience.
- roleplay-ask: sets a situation and asks the test taker to ask three or four questions.
- roleplay-solve: sets a problem and asks the test taker to explain it and offer alternatives.
- roleplay-experience: asks about a similar experience the test taker had.
- comparison: asks to compare past and present, or two things, about the topic.
- social-issue: asks about a news item, trend or concern related to the topic.

Rules:
- Each run of description, routine and past-experience is a set of three questions on one topic. Take set topics from the survey topics. One set may use a common topic outside the survey.
- Do not reuse a topic or situation from the questions already asked.
- Role-play questions share one situation. Comparison and social-issue questions share one topic.
- topic: the survey topic the question is about, "self" for self-intro, or a short name for a topic outside the survey.
- The difficulty change applies to questions 8 and later. Higher levels and "harder" get longer questions with two or three parts. Lower levels and "easier" get short, simple questions.
- For weak types, ask the same kind of question again with a different topic so the test taker can practise it.
- Return JSON that follows the response schema.
