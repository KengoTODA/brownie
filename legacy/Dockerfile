FROM java:8-jre
MAINTAINER Kengo TODA skypencil@gmail.com


# Install ffmpeg
# https://trac.ffmpeg.org/wiki/CompilationGuide/Ubuntu

ENV FFMPEG_SOURCE /etc/ffmpeg/ffmpeg_sources
ENV FFMPEG_BUILD /etc/ffmpeg/ffmpeg_build
RUN mkdir -p $FFMPEG_SOURCE
RUN mkdir -p $FFMPEG_BUILD

RUN apt-get update
RUN apt-get -y --force-yes install autoconf automake build-essential libass-dev libfreetype6-dev \
  libsdl1.2-dev libtheora-dev libtool libva-dev libvdpau-dev libvorbis-dev libxcb1-dev libxcb-shm0-dev \
  libxcb-xfixes0-dev pkg-config texinfo zlib1g-dev

RUN apt-get install yasm

RUN cd $FFMPEG_SOURCE && \
  wget https://storage.googleapis.com/downloads.webmproject.org/releases/webm/libvpx-1.4.0.tar.bz2 && \
  tar xjvf libvpx-1.4.0.tar.bz2
RUN cd $FFMPEG_SOURCE/libvpx-1.4.0 && \
  PATH="$HOME/bin:$PATH" ./configure --prefix="$FFMPEG_BUILD" --disable-examples --disable-unit-tests && \
  PATH="$HOME/bin:$PATH" make && \
  make install && \
  make clean

RUN cd $FFMPEG_SOURCE && \
  wget https://ffmpeg.org/releases/ffmpeg-2.8.1.tar.bz2 && \
  tar xjvf ffmpeg-2.8.1.tar.bz2
RUN cd $FFMPEG_SOURCE/ffmpeg-2.8.1 && \
  PATH="$HOME/bin:$PATH" PKG_CONFIG_PATH="$FFMPEG_BUILD/lib/pkgconfig" ./configure \
    --prefix="$FFMPEG_BUILD" \
    --pkg-config-flags="--static" \
    --extra-cflags="-I$FFMPEG_BUILD/include" \
    --extra-ldflags="-L$FFMPEG_BUILD/lib" \
    --bindir="$HOME/bin" \
    --enable-libvpx && \
  PATH="$HOME/bin:$PATH" make && \
  make install && \
  make distclean && \
  hash -r
RUN ln -s $HOME/bin/ffmpeg /usr/local/bin/

# Install brownie

RUN mkdir -p /etc/brownie
COPY target/legacy-*.jar /etc/brownie/brownie.jar
COPY application-conf.json /etc/brownie/application-conf.json

# EXPOSE HTTP (8080) and Hazelcast ports
# https://github.com/vert-x3/vertx-hazelcast/blob/3.1.0/src/main/resources/default-cluster.xml
EXPOSE 5701 8080 54327/udp

CMD ["/usr/bin/java", "-jar", "/etc/brownie/brownie.jar", "-cluster", "-conf", "/etc/brownie/application-conf.json"]
