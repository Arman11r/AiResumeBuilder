
echo "Starting eureka-server..."
cd eureka-server
nohup mvn spring-boot:run > ../eureka.log 2>&1 &
cd ..

sleep 15

for service in api-gateway auth-service ai-service export-service jobmatch-service notification-service resume-service section-service template-service; do
    echo "Starting $service..."
    cd $service
    nohup mvn spring-boot:run > ../$service.log 2>&1 &
    cd ..
done

echo "All backend services started."
