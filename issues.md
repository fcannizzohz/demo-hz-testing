1. there seems to be a dependency on log4j
2. if factory/instances not shutdown in test, an assertion fails requiring dependency on assert4j
3. even if user uses junit5, junit4 dependencies need to be included
4. @Repeat doesn't seem to work as expected and tests don't get executed the expected number of times
5. 