1. there seems to be a dependency on log4j
2. if factory/instances not shutdown in test, an assertion fails requiring dependency on assert4j
3. even if user uses junit5, junit4 dependencies need to be included
