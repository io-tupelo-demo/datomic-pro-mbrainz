#!/bin/bash -v 

cd ~/opt/datomic
export XMX=-Xmx4g

bin/transactor   -Ddatomic.printConnectionInfo=true   config/dev-transactor-template.properties 


