FROM hseeberger/scala-sbt

ADD . /usr/src/dbstress/
WORKDIR /usr/src/dbstress/

RUN sbt packArchive

ENTRYPOINT [ "./target/pack/bin/dbstress" ]
