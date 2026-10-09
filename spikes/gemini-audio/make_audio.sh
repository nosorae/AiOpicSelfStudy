#!/usr/bin/env bash
# 테스트 음성을 만든다. macOS say 합성 음성이다(사람 녹음 아님).
# 세 부분 사이에 무음을 넣어 멈춤 위치를 알고 있게 한다. 실제 위치는 silencedetect 로 다시 잰다.
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p audio/chunks && rm -f audio/chunks/* audio/*.aiff
say -v Samantha -r 120 -o audio/p1.aiff "Hi, my name is Alex and I am a software developer living in Seoul. I usually work from home three days a week, and on the other days I go to an office near Gangnam station. In my free time I like to go jogging along the Han river, especially in the early morning when the air is fresh. After jogging I usually make a simple breakfast with eggs and toast."
say -v Samantha -r 120 -o audio/p2.aiff "Last weekend I went to a small cafe with my friends. We talked about our trip to Busan last summer. We ate a lot of seafood and we walked on the beach until midnight. It was one of the best trips I have ever had. I took so many pictures that my phone ran out of storage."
say -v Samantha -r 120 -o audio/p3.aiff "These days I am trying to improve my English speaking skills because I want to work with people from other countries. I practice every evening for about thirty minutes, and I think my confidence is getting better little by little. My goal is to get a good score on my speaking test next month."
ffmpeg -loglevel error -y -f lavfi -t 1.5 -i anullsrc=r=22050:cl=mono audio/s15.aiff
ffmpeg -loglevel error -y -f lavfi -t 2.0 -i anullsrc=r=22050:cl=mono audio/s20.aiff
ffmpeg -loglevel error -y -i audio/p1.aiff -i audio/s15.aiff -i audio/p2.aiff -i audio/s20.aiff -i audio/p3.aiff \
  -filter_complex "[0:a][1:a][2:a][3:a][4:a]concat=n=5:v=0:a=1" -ar 16000 -ac 1 audio/full.wav
afconvert -f m4af -d aac -b 32000 audio/full.wav audio/full.m4a
ffmpeg -loglevel error -y -i audio/full.wav -f segment -segment_time 5 audio/chunks/c%02d.wav
for w in audio/chunks/*.wav; do afconvert -f m4af -d aac -b 32000 "$w" "${w%.wav}.m4a"; rm "$w"; done
rm -f audio/*.aiff
ffprobe -v error -show_entries format=duration -of csv=p=0 audio/full.wav
ffmpeg -hide_banner -i audio/full.wav -af silencedetect=noise=-40dB:d=0.8 -f null - 2>&1 | grep silence_
ls audio/chunks | wc -l
rm audio/full.wav  # 커밋하지 않는다. 멈춤 위치는 위 출력에서 analyze.py 로 옮겼다
