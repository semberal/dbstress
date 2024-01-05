FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.8.2_3.2.2

ADD . /usr/src/dbstress/
WORKDIR /usr/src/dbstress/

RUN sbt packArchive

ENTRYPOINT [ "./target/pack/bin/dbstress" ]
