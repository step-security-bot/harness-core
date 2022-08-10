## requires yq 3.3.2 to be installed
## installation command in mac: wget https://github.com/mikefarah/yq/releases/download/3.3.2/yq_darwin_amd64 -O /usr/local/bin/yq
## ngrok should be added to $PATH variable for global access

## this script checks if
##  - redis is running. If not running starts it.
##  - Kills already running ngrok and starts a new process and updates the config.yml with delegate status endpoint.
##  - updates config overrides in ci-config-manager.yml and ng-manager config.yml

home_directory=$(git rev-parse --show-toplevel)
cd $home_directory
printf "home directory is %s\n" $home_directory

printf "checking for running ngrok\n"
ngrok_pid=$(ps -ef | grep ngrok | grep -v grep | awk '{print $2}')
if [ -n "$ngrok_pid" ]; then
    printf "killing ngrok with pid %s\n" $ngrok_pid
    kill -9 $ngrok_pid
else
    printf "no running ngrok found\n"
fi
printf "starting new ngrok process\n"
ngrok tcp 8080 > /dev/null &
sleep 5

printf "checking for running redis\n"
redis_pid=$(ps -ef | grep redis | grep -v grep | awk '{print $2}')
if [ -n "$redis_pid" ]; then
    printf "redis is running pid %s\n" $redis_pid
else
    redis-server /usr/local/etc/redis.conf > /dev/null &
fi

public_url=$(curl -s localhost:4040/api/tunnels | jq -r ".tunnels[0].public_url" | sed 's/\\n/\n/g')

ngrok_pid=$(ps -ef | grep ngrok | grep -v grep | awk '{print $2}')
if [ -n "$ngrok_pid" ]; then
    printf "public url of ngrok is: %s\n" $public_url
else
    printf "failed to start ngrok\n"
    exit 1
fi

ngrok_host=$(echo "$public_url" | sed 's/tcp:\/\///')

ci_config_file=332-ci-manager/config/ci-manager-config.yml
ng_manager_config_file=120-ng-manager/config.yml
printf "Updating ci-manager config.yml\n"
yq write -i $ci_config_file shouldConfigureWithPMS "true"
yq write -i $ci_config_file ciExecutionServiceConfig.delegateServiceEndpointVariableValue "$ngrok_host"
yq write -i $ci_config_file ciExecutionServiceConfig.isLocal "true"

printf "Updating ng-manager config.yml\n"
yq write -i $ng_manager_config_file shouldConfigureWithPMS "true"