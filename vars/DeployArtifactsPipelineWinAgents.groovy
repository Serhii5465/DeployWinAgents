def call(Map pipeline_param){
    def String [] hosts_online;
    
    pipeline {
        agent { label 'master' }
        
        options { 
            skipDefaultCheckout() 
        }

        parameters {
            activeChoice choiceType: 'PT_SINGLE_SELECT', 
            filterLength: 1, filterable: false,
            name: 'GROUP_WIN_AGENTS',
            script: groovyScript(fallbackScript: [classpath: [], oldScript: '', sandbox: false, script: ''], 
            script: [classpath: [], oldScript: '', sandbox: false, 
            script: 'return [\'win-agent-host\', \'win-agent-lan\']'])

            reactiveChoice choiceType: 'PT_CHECKBOX', filterLength: 1, filterable: false, 
            name: 'WIN_AGENTS', referencedParameters: 'GROUP_WIN_AGENTS', 
            script: groovyScript(fallbackScript: [classpath: [], oldScript: '', sandbox: false, script: ''], 
            script: [classpath: [], oldScript: '', sandbox: false, 
            script: '''
            def GetNodesByLabel(String label){
                def nodes = []
                jenkins.model.Jenkins.get().computers.each { c ->
                if (c.node.labelString.contains("${label}")) {
                    nodes.add(c.node.selfLabel.name)
                }}
                return nodes
            }

            if (GROUP_WIN_AGENTS.equals("win-agent-host")) {
                return GetNodesByLabel(\'windows_agents-host\')
            }
            if (GROUP_WIN_AGENTS.equals("win-agent-lan")) {
                return GetNodesByLabel(\'windows_agents-lan\')
            }
            '''])
        }

        stages {
            stage('Ping agent'){
                steps{
                    script {
                        if(params.WIN_AGENTS.isEmpty()){
                            error("Select at least one host")
                        } else {
                            hosts_online = params.WIN_AGENTS.split(',')
                            for (item in hosts_online) {
                                CheckAgent(item)
                            }
                        }   
                    }
                }
            }

            stage('Checkout git'){
                steps {
                    git branch: pipeline_param.git_branch, 
                    poll: false, 
                    url: pipeline_param.git_repo_url
                }
            }
        }
    }
}