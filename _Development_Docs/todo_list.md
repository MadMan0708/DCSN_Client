- checking, if System.console() is available and if yes, start also classic console is not working
- fix problem with synchronization, one mistake is if calculation on client is started and project is uploaded after then,
  commnand for starting calculation has to be typed again
- when project is paused or canceld, tasks has to be remover from tasksInProgress list
- cancel project on client command is not working if project is completed but not in list for download on server
- show message on client that the archive is not supported if it unsupported extension
- check if given project is valid project
- todo load information about command from external xml file, so they are not part of the code
  , from xml file creates object and these object use with printing out info about commands
- set priority ranges which will be available
- in Client class, if tasks is canceled during calculation, the exception needs to be handled !!
- in Checker class
- problem with putting tasks to task pool on server after failure, for example:
  change timeout of client active pings to higher number, maybe 3 minuted, becouse as a assumption, tasks should take about
  15 minutes, so it the client will disconnect during that 3 minutes but connected immediatelu ( only network error), then there
  is no need to cancel tasks association for tasks calculated by this client
- implement server state saving - pause all calculation, wait for rest of tasks and synchronize all information to disk
