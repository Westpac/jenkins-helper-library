import groovy.json.JsonOutput

def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    stage ('Setting Up Deployment File') {
        
        def deployment_map = withCredentials(
            [
                [ // Azure Credentials  - Hard Coded for POC
                    $class: 'AzureCredentialsBinding', 
                    credentialsId: 'NProd-POC-Tenant',
                    clientIdVariable: "AZURE_CLIENT_ID", 
                    clientSecretVariable: 'AZURE_CLIENT_SECRET', 
                    subscriptionIdVariable: 'AZURE_SUBSCRIPTION_ID', 
                    tenantIdVariable: 'AZURE_TENANT_ID'
                ],
                [ // Terraform Enterprise 2 Credentials
                    $class: 'StringBinding', 
                    credentialsId: 'Terraform_Enterprise_POC_Token',
                    variable: 'TERRAFORM_SECRET'
                ],
                [ // ServiceNow
                    $class: 'UsernamePasswordMultiBinding', 
                    credentialsId: 'jenkins_servicenow',
                    usernameVariable: 'JENKINS_SNOW_USERNAME',
                    passwordVariable: 'JENKINS_SNOW_PASSWORD'
                ]
            ]
        ) {
            def deployment_map = [:]
            
            deployment_map['deployment':['id']] = config.app_id
            deployment_map['deployment':['component_name']] = config.app_component_name
            deployment_map['deployment':['environment']] = config.app_environment

            deployment_map['consul':['consul']] = "consul.australiaeast.cloudapp.azure.com"
            deployment_map['consul':['port']] = "8500"
            deployment_map['consul':['dc']] = "australiaeast"
            deployment_map['consul':['token']] = ""

            deployment_map['service_now':['url']] = "https://wbchpaaspoc.service-now.com/api/now/table/change_request"
            deployment_map['service_now':['username']] = env.JENKINS_SNOW_USERNAME
            deployment_map['service_now':['password']] = env.JENKINS_SNOW_PASSWORD

            deployment_map['azure_secret'] = env.AZURE_CLIENT_SECRET
            deployment_map['atlas_secret'] = env.TERRAFORM_SECRET
        
            return deployment_map
        }
        def jsonMap = readJSON text: JsonOutput.toJson(deployment_map)
        return jsonMap
    }
}
