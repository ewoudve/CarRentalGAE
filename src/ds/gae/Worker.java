package ds.gae;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.Quote;
import ds.gae.entities.Reservation;
import ds.gae.entities.TaskStatus;

public class Worker extends HttpServlet {
	private static final long serialVersionUID = -7058685883212377590L;
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		List<Quote> quotes = parseQuotes(req);
		
    	List<Reservation> done = new ArrayList<Reservation>();

		try {
			for (Quote quote : quotes) {
				done.add(confirmQuote(quote));
			}
			TaskStatus status = new TaskStatus(quotes.get(0).getCarRenter(), "RESERVATION(S) SUCCESSFUL");
			EntityManager em = EMF.get().createEntityManager();
			CarRentalModel.get().addTaskStatus(status);
			em.persist(status);
			em.close();
		} catch (ReservationException re) {
			EntityManager em = EMF.get().createEntityManager();
			for (Reservation r : done) {
				CarRentalCompany crc = em.find(CarRentalCompany.class,
						r.getRentalCompany());
				crc.cancelReservation(r);
			}
			em.close();
			
			EntityManager em2 = EMF.get().createEntityManager();
			TaskStatus status = new TaskStatus(quotes.get(0).getCarRenter(), re.getMessage());
			CarRentalModel.get().addTaskStatus(status);
			em2.persist(status);
			em2.close();
		}
	}
	
	/**
	 * Confirm the given quote.
	 *
	 * @param 	q
	 * 			Quote to confirm
	 * 
	 * @throws ReservationException
	 * 			Confirmation of given quote failed.	
	 */
	private Reservation confirmQuote(Quote q) throws ReservationException{
		EntityManager em = EMF.get().createEntityManager();
    	CarRentalCompany crc = em.find(CarRentalCompany.class, q.getRentalCompany());
        Reservation r = crc.confirmQuote(q);
        em.persist(r);
        em.close();
        return r;
	}
	
	private List<Quote> parseQuotes(HttpServletRequest req){
		ArrayList<Quote> quotes = new ArrayList<Quote>();
		int amountOfQuotes = Integer.parseInt(req.getParameter("amountOfQuotes"));
		
		for(int index=0; index<amountOfQuotes; index++){
			String startDate = req.getParameter("startDate"+index);
			DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
			Date sDate = null;
			try {
				sDate = format.parse(startDate);
			} catch (ParseException e2) {
				e2.printStackTrace();
			}

			String endDate = req.getParameter("endDate"+index);
			Date eDate = null;
			try {
				eDate = format.parse(endDate);
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
			
			String company = req.getParameter("company"+index);
			String renter = req.getParameter("carRenter"+index);
			String type = req.getParameter("carType"+index);
			Double price = Double.parseDouble(req.getParameter("price"+index));
			
			Quote q = new Quote(renter, sDate, eDate, company, type, price);
			
			quotes.add(q);
		}
		
		
		
		return quotes;
	}
}
