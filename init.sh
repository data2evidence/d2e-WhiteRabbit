# start a virtual display server 
Xvfb :1 &
# set the value of java.awt.display to the display server port
java ${JAVA_OPTS} -Djava.awt.headless=false -Djava.awt.display=:1 -jar /app.jar ${0} ${@}