### Eclipse and Websphere Developer Tools

If you haven't already: install the [Websphere Developer Tools](https://developer.ibm.com/wasdev/downloads/liberty-profile-using-eclipse/)


#### Clone the Git Repo

1. Open the Git repositories view
  -  Window -> Show View -> Other...
  - Type git in the filter box and select Git Repositories and click 'OK'
  - Click on Clone a Git Repository
2. Copy and paste the Github repository URI (https://github.com/WASdev/sample.rtcomm.sip.gateway.git) to the Location URI text field on Eclipse
3. Click 'Next', select only the 'master' branch, uncheck the other branches that may have been selected and Click 'Next'
4. Click on 'Finish'


#### Building with Maven

Import Maven projects into WDT

1. In the Git Repositories view, expand the rtcomm-sip-gateway repo
2. Right-click on the "Working Directory" folder and select "Copy Path to Clipboard"
3. Select File -> Import -> Maven -> Existing Maven Projects, then click 'Next'
4. In the Root Directory textbox, Paste in the repository directory (from Step 2)
5. Select Browse... and confirm 4 pom.xml files are selected, click "Finish"
6. Eclipse will create 4 projects:
  + rtcomm-sip-gateway-application - Web application
  + rtcomm-sip-gateway-wlpcfg - Liberty server configuration
  + sample.rtcomm.sip.gateway - Root directory of the sample

###### Run Maven Install

1. Right-click on sample.rtcomm.sip.gateway/pom.xml
2. Run As -> Maven Build...
3. In the Goals section enter "install"
4. Click 'Run', the build takes a few minutes to complete.

#### Running the Application Locally

For the purposes of this sample, we will create the Liberty server (step 3 in the wasdev.net instructions) a little differently to create and customize a Runtime Environment that will allow the server to directly use the configuration in the rtcomm-sip-gateway-wlpcfg project.

###### Create a Runtime Environment in Eclipse

Before creating a server you need a runtime environment for your server to be based on.

1. Open the 'Runtime Explorer' view:
    * *Window -> Show View -> Other*
    * type `runtime` in the filter box to find the view (it's under the Server heading).
2. Right-click in the view, and select *New -> Runtime Environment*
3. Give the Runtime environment a name, e.g. `wlp-2016.2.0.0` if you're using the June 2015 beta.
4. Either:
    * Select an existing installation (perhaps what you downloaded earlier, if you followed those instructions), or
    * Select *Install from an archive or a repository* to download a new Liberty archive.
5. If you chose to install from an archive or repository and you choose to download and install a new runtime environment: Choose _WAS Liberty V8.5.5.9 with Java EE 7 Web Profile_.
6. Continue through the prompts.

###### Run the Maven Build on Your Runtime Environment

Copy the path to your Runtime Environment for this step.

1. Right-click on rtcomm-sip-gateway-wlpcfg/pom.xml
2. Run As -> Maven Build...
3. In the Goals section enter
  ```text
  install -Dwlp.install.dir=[PATH_TO_RUNTIME_ENVIRONMENT]
  ```
4. Click 'Run'

This will install the required features to the Runtime Environment.


###### Add the User directory from the Maven project, and create a server

1. Right-click on the Runtime Environment created above in the 'Runtime Explorer' view, and select *Edit*
2. Click the `Advanced Options...` link
3. If the `rtcomm-sip-gateway-wlpcfg` directory is not listed as a User Directory, we need to add it:
    1. Click New
    2. Select the `rtcomm-sip-gateway-wlpcfg` project
    3. Select *Finish*, *OK*, *Finish*
4. Right-click on the `rtcomm-sip-gateway-wlpcfg` user directory listed under the target Runtime Environment in the Runtime Explorer view., and select *New Server*.
5. The resulting dialog should be pre-populated with the rtcommSipGatewayServer Liberty profile server.
6. Click *Finish*

_Note_: If the 'rtcomm-sip-gateway-wlpcfg' project does not appear in the dialog to add a project to the User Directory make sure there is a 'shared' folder in the rtcomm-sip-gateway-wlpcfg project with an empty .gitkeep file inside it.

###### Running Liberty and the Application From WDT
1. Select the `rtcomm-sip-gateway-application` project
2. Right-click -> *Run As... -> Run On Server*
3. Select the "WebSphere Application Server under localhost" folder, and select *Finish*

_Note_: If you get validation errors re-deploy the application.
