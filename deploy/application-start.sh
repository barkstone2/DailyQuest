CONTAINER_NAME=todayquest-server
REPOSITORY=barkstone2/todayquest-server
VERSION=$(cat /home/ec2-user/app/server/version.txt)

ls -la
docker pull $REPOSITORY:$VERSION
/home/ec2-user/app/server/container-start.sh $CONTAINER_NAME $REPOSITORY $VERSION