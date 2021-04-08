FROM ubuntu
RUN apt update
RUN apt upgrade
RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN apt install curl gnupg gnupg2 gnupg1 default-jre default-jdk -y
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt update
RUN apt install git sbt -y
RUN git clone https://github.com/PedroAraoz/distribuidos.git dist
RUN cd dist/ && sbt clean compile
EXPOSE 50000

# CMD ["/bin/bash"]
