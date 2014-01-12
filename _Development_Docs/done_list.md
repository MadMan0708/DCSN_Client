12.1.2014
- commenting and refactoring code
- cancelling project is not working if project is completed but not in list for download on server

11.1.2014
- archive correctness checking

7.1.2014
- tasks are launched in separated JVM with memory and other parameters set

28.12.2013
 - broadcasting discovering of server implemented on client

28.11.2013
 - default values for server address, port, download address and upload address
 - BUG fixed: list all could be started without established connection to server, it shows message if no projects are on the server
 - calculation could be started before established connection as well as various other commands, it is not able now. It user try to use these commands before connection, message is shown
 - setServerAddress now takes IP address and hostname as parameter and checks it for correctness ( It solves the bug when application freezing when incorrect IP address was written), port can be added after colon. Like host|ip address:port
 - connect take IP address and port as well. If it connects with these values, these values are stored to properties files. Otherwise it connects with values from properties file
 - non-interactive mode added : program can be started without gui and program can be started with project jar parameter which will be calculated and application will close immediately after calculation is done
 - upload dir added. If user writes only file names instead of paths to commands which loads jar files and data files, it will try to load these files from upload folder.
 - classes renamed: ClienApiWithoutLog renamed to StandartRemoteProvider
                    ClientApi renamed to RemoteProvider
                    InternalAPI renamed to Connector
                    InternalAPIWithLog deletead, as the code was moved to client class
- method for getting information about one project was added - PrintProjectInfo
- switch with command identification was replaced by class ClientCommands, commands are identified by Java Reflection API
- method CreateProjectFrom implemented, it allows create project from previous project jar with new name
- message is shown when user is logged to the system and has some projects in progress
- user can not be connected if another user with same name is now in the system now
- problem with closing application fixed, it closes java properly now
- multiple lines to output fixed - it was problem with logging system, which was fixed
- if client now writes the command stopCalculation, program will stop recieving new tasks but will finish it's current tasks. If client closes the application, the tasks calculation is cancelled immediately.
- client GUI shrinking was fixed
- problem with commands pause, resume and cancel was fixed, project can now be manipulated with by using these commands

19.11.2013
- library path made relatives

12.11.2013
- class loading system has changed. Now instead of one class, user upload jar file with classes needed for computation and classes needed for project preparation before it's uploaded and after it's downloaded


4.11.2013
- fix bug with classFormatException truncate error during automatic calculation
- fix different time padding in log

3.11.2013:
- create interface on client side: automatic recieving of task, changing it and sending to server

21.10.2013:
- text in log in GUI console are now intended evenly
- project is removed from the server if is successfully downloaded
- method downloadProject is overloaded to specify name of the downloaded file
- parameter downloadedFileName added to Downloader constructor class, instead of creating name inside class

19.10.2013:
- Class project has been commented, data structures was  changed to achieve more efficiency and code was changed to 			 	                      meet concurrency problems
- method addClient in IServerImpl was deleted as it won't be needed because of rewriting of logging for concurrency purposes
- Extraction of project after uploading was changed, class Extractor created
- Class server was split and made more clear
- classManager class was made more concurrency stable
- Logging was't concurrency stable, two users with same name could be logged at same time

=============

13.10.2013:
- handling unknown command in console
- check correct number of command parameters in console
- Time is not changing in logs 
- Make log and history windows in GUI console bigger
- Implement closing of GUI console
- Implement history in GUI console - up and down
- Add message after start command is written on server
- after disconnection it looks like the last user is still on the server
- After session is closed, remove client from active clients and remove classloader associated with this session
- In Client checker firstly ask the server which compute class is needed to proceed next task,
  so client can download the class data and set correct classloader
- Implement cache for class data in classloaders
- Implement class projectUID - project is identified by owner and name	

=============

12.10.2013:
- Problem with uploding project on client solved - progress of uploading wasn't seen
- On client, thread management was changed to use Exetutors and Callable<T>
- Add path validity testing in path which are written to consoles
- Implement GUI console, which will be devided into 3 parts, input, input history and logging window

=============

29.09.2013:
- automatic creation of upload, projects, server_basedir folders
- repair and implement properties storing on clint and server
- initialize some variables from properties file at start of client or server
- change texts on client and server side
- Class Logger was rewrited, logging to file implemented
- Datum and time is now part of the log messages

=============
	
Issues solved before draft handling in:        

- when is tasks unfinished for some reason , it is needed to place it back into list uncompletedTask in project,
  implement timeout


- ClassLoader problems fixed:
	- there is no need to have special folder where all compute classes are stored
	- no extra folder has to be added into classpath before starting program
	- no extra rules about the name of compute class, f.e like format before ClientID_ProjectID_Task.class
    
