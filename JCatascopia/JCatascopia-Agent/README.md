JCatascopia-Agent
====================================

Prerequisites
---------------

- **Software Prerequisites** 

JCatascopia-Agent only requires Java to be installed. Recommended versions are 1.6.x and 1.7.x

Note: if you will be building JCatascopia via command line tools then git and mvn are required as well

- **Network Prerequisites** 

JCatascopia-Agent uses TCP as its default distribution network protocol. Ports 4242, 4243 and 4245 must be opened and made available

Licence
---------------
The complete source code of the JCatascopia Monitoring System is open-source and available to the community under the [Apache 2.0 licence](http://www.apache.org/licenses/LICENSE-2.0.html)

Getting Started
---------------
Note: if you have an IDE supporting git repository imports and maven then skip steps 1 and 2

- Step 1: Import the JCatascopia git repository to your IDE or clone the repository:

```shell
git clone https://github.com/dtrihinas/JCatascopia.git
```

- Step 2: Build via maven the JCatascopia components:

```shell
mvn clean install
```

- Step 3: JCatascopia Agents are packaged as a tarball, extract the contents of the tarball:

```shell
tar xvfz JCatascopia-Agent-<VERSION>-SNAPSHOT.tar.gz
```

- Step 4: Enter the extracted directory and execute the installer *with root permission*:

```shell
cd JCatascopia-Agent-<VERSION>-SNAPSHOT
./installer
```

- Step 5: The JCatascopia Agent is pre-configured with default properties. However, if the JCatascopia-Server is not at localhost then at least the server.endpoint property must change 

```shell
vi /usr/local/bin/JCatascopiaAgentDir/resources/agent.properties
server.endpoint=IP_OF_JCATSCOPIA_SERVER
```

- Step 6: Starting the JCatascopia-Agent:

```shell
/etc/init.d/JCatascopia-Agent start
```

- Step 7: Stopping the JCatascopia-Agent:

```shell
/etc/init.d/JCatascopia-Agent stop
```

Note
---------------
This version is the standalone version of JCatascopia. For the [CELAR](http://celarcloud.eu/) project compliant version please have a look [here] (https://github.com/CELAR/cloud-ms)

Contact Us
---------------
Please contact Demetris Trihinas trihinas{at}cs.ucy.ac.cy for any issue

Publications
---------------
For any research work in which JCatascopia is used, please cite the following article:

"JCatascopia: Monitoring Elastically Adaptive Applications in the Cloud", D. Trihinas and G. Pallis and M. D. Dikaiakos, "14th IEEE/ACM International Symposium on Cluster, Cloud and Grid Computing" (CCGRID 2014), Chicago, IL, USA 2014
http://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=6846458&isnumber=6846423

Website
---------------
[http://linc.ucy.ac.cy/CELAR/jcatascopia](http://linc.ucy.ac.cy/CELAR/jcatascopia)

