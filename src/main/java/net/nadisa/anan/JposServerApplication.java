package net.nadisa.anan;

import org.jpos.q2.Q2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JposServerApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(JposServerApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Q2 q2=new Q2();
		q2.start();
		
	}

}