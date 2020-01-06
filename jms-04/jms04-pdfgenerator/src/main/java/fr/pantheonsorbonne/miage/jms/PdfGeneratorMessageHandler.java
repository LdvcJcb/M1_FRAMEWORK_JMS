package fr.pantheonsorbonne.ufr27.miage.jms;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import fr.pantheonsorbonne.ufr27.miage.DiplomaInfo;
import fr.pantheonsorbonne.ufr27.miage.dto.BinaryDiplomaDTO;

@ApplicationScoped
public class BinaryDiplomaManager implements Closeable {

	@Inject
	@Named("diplomaRequests")
	private Queue requestsQueue;

	@Inject
	@Named("diplomaFiles")
	private Queue filesQueue;

	@Inject
	private ConnectionFactory connectionFactory;

	private Connection connection;
	private MessageConsumer binDiplomaConsumer;
	private MessageProducer diplomaRequestProducer;

	private Session session;

	@PostConstruct
	void init() {
		try {
			connection = connectionFactory.createConnection("nicolas", "nicolas");
			connection.start();
			session = connection.createSession();
			binDiplomaConsumer = session.createConsumer(filesQueue);
			diplomaRequestProducer = session.createProducer(requestsQueue);
		} catch (JMSException e) {
			throw new RuntimeException(e);
		}

	}

	public BinaryDiplomaDTO consume() {

		// receive a Byte Message from the consumer
		// create a byte array sized after the message's payload body length
		// read the message on the byte array
		// create a BinaryDiplomaDTO containing the id of the diploma and the data
		// return the DTO

		try {
			BytesMessage msg = (BytesMessage)binDiplomaConsumer.receive();
			byte[] data = new byte[(int) msg.getBodyLength()];
			msg.readBytes(data);
			BinaryDiplomaDTO binDip = new BinaryDiplomaDTO ();
			binDip.setId(msg.getIntProperty("id"));
			binDip.setData(data);
			return binDip;

		} catch (JMSException e) {
			System.out.println("FAILED TO CONSUME THE MESSAGE");
			return null;
		}

		
	}

	public void requestBinDiploma(DiplomaInfo info) {

		// create a String writer
		// create a JaxBContext, and bount DiplomaInfo.class
		// create a Marshaller and marshall the class in the writer
		// send a text Message containing the JAXB-marshalled object through the wire
		try {
			StringWriter sw = new StringWriter();
			JAXBContext jxbc = JAXBContext.newInstance(DiplomaInfo.class);
			jxbc.createMarshaller().marshal(info, sw);
			this.diplomaRequestProducer.send(this.session.createTextMessage(sw.toString()));
		} catch (JAXBException | JMSException e) {
			// TODO Auto-generated catch block
			System.out.println("Request failed");		}	
	}

	@Override
	public void close() throws IOException {
		try {
			binDiplomaConsumer.close();
			session.close();
			connection.close();
		} catch (JMSException e) {
			System.out.println("Failed to close JMS resources");
		}

	}

}
