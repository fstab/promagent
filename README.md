添加host
199.232.68.133 raw.githubusercontent.com 



application.yml 添加
promagent.agent.appName=appName
promagent.agent.appEvn=dev



添加hook.yml
见promagent-log-spring hook.yml




pom 添加
		<dependency>
			<groupId>io.promagent</groupId>
			<artifactId>promagent-log-spring</artifactId>
			<version>2.0-SNAPSHOT</version>
		</dependency>
		
	
	<repository>
		<id>promagent</id>
		<url>https://raw.githubusercontent.com/javazhangyi/promagent/master</url>
		<snapshots>
			<enabled>true</enabled>
			<updatePolicy>always</updatePolicy>
		</snapshots>
	</repository>
