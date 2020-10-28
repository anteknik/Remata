package net.nadisa.anan;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.MUX;
import org.jpos.q2.Q2;
import org.jpos.q2.iso.QMUX;
import org.jpos.util.NameRegistrar.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class JposClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(JposClientApplication.class, args);
	}

	@Bean
	public Q2 q2() {
		Q2 q2 = new Q2();
		q2.start();
		return q2;

	}

	@Bean
	public MUX mux(Q2 q2) throws NotFoundException {
		while (!q2.ready()) {
			ISOUtil.sleep(10);
		}
		return QMUX.getMUX("my-mux");

	}
	
	@Autowired
	private MUX mux;
	
	@GetMapping("echo")
	public String echo() throws Exception {
		ISOMsg msg = new ISOMsg();
		msg.setMTI("0800");
		msg.set(11, "000001");
		msg.set(70, "301");
		ISOMsg respMsg = mux.request(msg, 3000);
		return respMsg.toString();
		
	}

}
