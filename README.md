# D<sub>ata</sub> P<sub>rofiling</sub> Q<sub>uery</sub> L<sub>anguage</sub>
## _Application-driven Data Profiling_

This is a project of the [University of Marburg](https://www.uni-marburg.de/) within the research group [Big Data Analytics](https://www.uni-marburg.de/en/fb12/research-groups/big_data_analytics) of [Prof. Dr. Thorsten Papenbrock](https://www.uni-marburg.de/en/fb12/research-groups/big_data_analytics/people/thorsten-papenbrock).

##### Goal
With DPQL we seek to expand the existing way on, how to discover dependencies and use data profiling in order to improve algorithm use in a way that minimises the problem space and enables user to handle data profiling in a common interface

## Features
The most importent features of the DPQL Project are:
- Custom SQL-Like language for solving data profiling tasks
- Recognizes, parsing, interpreting and executing of DPQL Statements
- Building concrete parse trees and execution trees
- Offering a way to build and extend new functionality on top of DPQL

## Installation

Requierements to install and use this project are:
- Java JDK 1.8 or later
- Maven 3.1.0
- Git

Used dependencies:
- [org.reflections](https://github.com/ronmamo/reflections)
- [antlr](https://github.com/antlr/antlr4)
- [akka](https://github.com/akka/akka)
- [junit](https://github.com/junit-team/junit4)
- [logback](https://github.com/qos-ch/logback)

To use this project:
1. put your data source in the data folder
2. write a DPQL statement following the [guidelines](https://github.com/SeegerM/DPQL/wiki)
3. read results in the console
