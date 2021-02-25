#!groovy

def buildFronetndApplication (String app) {
    dir("angular/") { 
        sh "npx nx run-many --target=test --projects=${app} --parallel=true --maxParallel=3" 
        sh "npx nx run-many --target=build --projects=${app} --parallel=true --maxParallel=3 --prod"
        sh "echo \"<pre>Jira version: $VERSION\n${git.getLastCommitInfo()}</pre>\" > ../rig_space_${app}/frontend/version.html"
    }                  
}

def buildBackendApplication (String app) {
    sh "mvn clean package -pl rig_space_${app} -am -P production"
}

def getValidationSchemas (String app) {
    def validationSchemesDir = "rig_space_${app}/target/distrib/validation_schemes"
    sh "mkdir $validationSchemesDir"
    sh "cp -r --force rigspace_customization/common/${app}/validation_schemes/* $validationSchemesDir/"
}

def getExportMapping (String app) {
    def exportMappingsDir = "rig_space_${app}/target/distrib/export_mapping"
    sh "mkdir $exportMappingsDir"
    sh "cp -r --force rigspace_customization/novatek/${app}/export_mapping/* $exportMappingsDir/"
}

def writeMD5SUMMfiles (String app) {
    sh "mkdir rig_space_$app/${TARGET}/checkSums"
    sh "md5sum `find rig_space_$app/${TARGET}/ -type f -name *.jar` | awk -F 'rig_space_$app/${TARGET}/' '{ print \$1\"\t\"\$2 }' >> rig_space_$app/${TARGET}/checkSums/backendMD5"
    sh "md5sum `find rig_space_$app/${TARGET}/frontend/ -type f` | awk -F 'rig_space_$app/${TARGET}/' '{ print \$1\"\t\"\$2 }' >> rig_space_$app/${TARGET}/checkSums/frontendMD5"
    sh "md5sum `find rig_space_$app/${TARGET}/ -type f -name *.sh` | awk -F 'rig_space_$app/${TARGET}/' '{ print \$1\"\t\"\$2 }' >> rig_space_$app/${TARGET}/checkSums/shMD5"
    sh "md5sum `find rig_space_$app/${TARGET}/ -type f -name *.json` | awk -F 'rig_space_$app/${TARGET}/' '{ print \$1\"\t\"\$2 }' >> rig_space_$app/${TARGET}/checkSums/validationMD5"
    sh "md5sum `find rig_space_$app/${TARGET}/ -type f -name *.jasper` | awk -F 'rig_space_$app/${TARGET}/' '{ print \$1\"\t\"\$2 }' >> rig_space_$app/${TARGET}/checkSums/jasperMD5"
}

def getTemplates (String app) {
    sh "cp -r --force rig_space_${app}/templates/* rig_space_${app}/target/distrib/templates/"
}

def cleanApplication () {
    echo "Clean node_modules and package-lock.json in angular folder's"
    def dirs = [
        "rig_space_admin/angular",
        "rig_space_equipment/angular",
        "rig_space_reporting/angular",
        "rig_space_analytics/angular",
        "rig_space_authentication/angular",
        "rig_space_common/angular",
        "rig_space_rating/angular",
        "rig_space_monitoring/angular",
        "angular/"
    ]

    dirs.each{ dir -> sh "rm -rf rig_space/$dir/{node_modules,package-lock.json}"}

    sh(script: "npm cache clean --force")
}

def deployAllApplications (String path_to, String environment) {
    echo "Deploy all applications"
    deployApplication("authentication", "$path_to", "$environment")
    deployApplication("admin", "$path_to", "$environment")
    
    // if (params.ANALYTICS) { 
	// 	deployApplication("analytics", "$path_to", "$environment")
    // }
    if (params.REPORTING) {
        deployApplication("reporting", "$path_to", "$environment")
    }
    if (params.EQUIPMENT) {
        deployApplication("equipment", "$path_to", "$environment")
    }
    if (params.RATING) {
    	deployApplication("rating", "$path_to", "$environment")
    }
    if (params.MONITORING) {
        deployApplication("monitoring", "$path_to", "$environment")
    }
}

def deployApplication (String app, String path_to, String environment) {
    echo "Deploy $app"
    sshPublisher(publishers: [sshPublisherDesc(
                            configName: "$environment",
                            transfers: [
                                // remove previous build
                                sshTransfer(
                                    excludes: '',
                                    execCommand: "rm -r --force ~/$path_to/$app/{lib,frontend,validation_schemes,export_mapping,templates}",
                                    execTimeout: 120000,
                                    flatten: false,
                                    makeEmptyDirs: false,
                                    noDefaultExcludes: false,
                                    patternSeparator: '[, ]+',
                                    remoteDirectory: '',
                                    remoteDirectorySDF: false,
                                    removePrefix: '',
                                    sourceFiles: ''
                                ),
                                // app distrib
                                sshTransfer(
                                    excludes: "rig_space_$app/target/distrib/resources/,rig_space_$app/target/distrib/${app}.sh",
                                    execCommand: "chmod +x ~/$path_to/$app/${app}.sh && ~/$path_to/$app/${app}.sh -a restart",
                                    execTimeout: 120000,
                                    flatten: false,
                                    makeEmptyDirs: false,
                                    noDefaultExcludes: false,
                                    patternSeparator: '[, ]+',
                                    remoteDirectory: "$path_to/$app",
                                    remoteDirectorySDF: false,
                                    removePrefix: "rig_space_$app/target/distrib/",
                                    sourceFiles: "rig_space_$app/target/distrib/",
                                    usePty: true
                                )],
                                usePromotionTimestamp: false,
                                useWorkspaceInPromotion: false,
                                verbose: false)])
}

def publishAllApplications (String app) {
    echo "Deploy all applications"
    publishApplication("authentication")
    publishApplication("admin")
    if (params.ANALYTICS) { 
        publishApplication("analytics")
    }
    if (params.REPORTING) {
        publishApplication("reporting")
    }
    if (params.EQUIPMENT) {
        publishApplication("equipment")
    }
    if (params.RATING) {
        publishApplication("rating")
    }
    if (params.MONITORING) {
        publishApplication("monitoring")
    }
}

def publishApplication (String app) {
    // remove previous build for choosen version
    echo "Send distrib $app"
        sshPublisher(publishers: [sshPublisherDesc(
                configName: 'distrib',
                transfers: [
                    sshTransfer (
                        excludes: '',
                        execCommand: "rm -r --force /var/www/distrib/$PROJECT_NAME/distrib/$VERSION/$app",
                        execTimeout: 120000,
                        flatten: false,
                        makeEmptyDirs: false,
                        noDefaultExcludes: false,
                        patternSeparator: '[, ]+',
                        remoteDirectory: '',
                        remoteDirectorySDF: false,
                        removePrefix: '',
                        sourceFiles: ''
                    ),
                    // publish distrib modules
                    sshTransfer (
                        excludes: '',
                        execCommand: "chmod +x /var/www/distrib/$PROJECT_NAME/distrib/$VERSION/$app/${app}.sh",
                        execTimeout: 120000,
                        flatten: false,
                        makeEmptyDirs: false,
                        noDefaultExcludes: false,
                        patternSeparator: '[, ]+',
                        remoteDirectory: "$PROJECT_NAME/distrib/$VERSION/${app}",
                        remoteDirectorySDF: false,
                        removePrefix: "rig_space_$app/target/distrib/",
                        sourceFiles: "rig_space_$app/target/distrib/"
                )],
                usePromotionTimestamp: false,
                useWorkspaceInPromotion: false,
                verbose: false)])       
}