#!/usr/bin/env groovy
package jira

def send(String buildResult, String projectName, String channel) {
  def tokensByChannel = [
    gtionline: 'CM8UQNVTP',
    rigspace: 'CM2GTRU0H',
    qa: 'CLXFS4RRR'
  ]
  if  ( buildResult == "FAILURE" ) {
    slackSend color: "danger", channel: tokensByChannel.get(channel), message: "Project - $projectName Job: ${env.JOB_NAME} with buildnumber ${env.BUILD_NUMBER} was FAILED"
  }
  else if ( buildResult == "UNSTABLE" ) {
    slackSend color: "warning", channel: tokensByChannel.get(channel), message: "Project - $projectName Job: ${env.JOB_NAME} with buildnumber ${env.BUILD_NUMBER} was UNSTABLE"
  }
}

def sendRC(String buildResult, String projectName, String channel) {
  
  def getIssuesSlackList = jira.getIssuesSlackList("$PROJECT_KEY","$VERSION");

  def tokensByChannel = [
    gtionline: 'CM8UQNVTP',
    rigspace: 'CM2GTRU0H',
    qa: 'CLXFS4RRR'
  ]
  if  ( buildResult == "SUCCESS" ) {
    slackSend color: "good", channel: tokensByChannel.get(channel), message: "[RELEASE] $projectName - $version \n $getIssuesSlackList"
  }
  else if ( buildResult == "FAILURE" ) {
    slackSend color: "danger", channel: tokensByChannel.get(channel), message: "Project - $projectName Job: ${env.JOB_NAME} with buildnumber ${env.BUILD_NUMBER} was FAILED"
  }
  else if ( buildResult == "UNSTABLE" ) {
    slackSend color: "warning", channel: tokensByChannel.get(channel), message: "Project - $projectName Job: ${env.JOB_NAME} with buildnumber ${env.BUILD_NUMBER} was UNSTABLE"
  }
}