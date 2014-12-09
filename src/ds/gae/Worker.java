package ds.gae;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.Quote;
import ds.gae.entities.Reservation;
import ds.gae.entities.ReservationConstraints;
import ds.gae.view.JSPSite;
import ds.gae.view.ViewTools;

public class Worker extends HttpServlet {
	private static final long serialVersionUID = -7058685883212377590L;
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String startDate = req.getParameter("startDate");
		DateFormat format = new SimpleDateFormat("EEE MMM dd hh:mm:ss zzz yyyy");
		Date sDate = null;
		try {
			sDate = format.parse(startDate);
		} catch (ParseException e2) {
			e2.printStackTrace();
		}

		String endDate = req.getParameter("endDate");
		Date eDate = null;
		try {
			eDate = format.parse(endDate);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		
		String company = req.getParameter("company");
		String renter = req.getParameter("carRenter");
		String type = req.getParameter("carType");
		Double price = Double.parseDouble(req.getParameter("price"));
		
		Quote q = new Quote(renter, sDate, eDate, company, type, price);
		
		EntityManager em = EMF.get().createEntityManager();
    	CarRentalCompany crc = em.find(CarRentalCompany.class, q.getRentalCompany());
        Reservation r = null;
		try {
			r = crc.confirmQuote(q);
		} catch (ReservationException e) {
			System.out.println(e.getMessage());
		}
		if(r!= null){
			em.persist(r);
			System.out.println("quote confirmed");
		}else{
			System.out.println("failed to confirm quote");
		}
        em.close();

        resp.sendRedirect(JSPSite.CREATE_QUOTES.url());
		
	}
}
