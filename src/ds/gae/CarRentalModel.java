package ds.gae;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

import ds.gae.entities.Car;
import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.CarType;
import ds.gae.entities.Quote;
import ds.gae.entities.Reservation;
import ds.gae.entities.ReservationConstraints;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.*;


public class CarRentalModel {
	
	public static HashMap<String, ReservationException> failedReservationsPerUser = new HashMap<String, ReservationException>();

	public Map<String,CarRentalCompany> CRCS = new HashMap<String, CarRentalCompany>();	

	private static CarRentalModel instance;

	public static CarRentalModel get() {
		if (instance == null)
			instance = new CarRentalModel();
		return instance;
	}

	/**
	 * Get the car types available in the given car rental company.
	 *
	 * @param 	crcName
	 * 			the car rental company
	 * @return	The list of car types (i.e. name of car type), available
	 * 			in the given car rental company.
	 */
	public Set<String> getCarTypesNames(String crcName) {
		EntityManager em = EMF.get().createEntityManager();
		Set<String> names = new HashSet<String>();
		Query query = em
				.createQuery(
						"SELECT crc.carTypes FROM CarRentalCompany crc WHERE crc.name=:company")
						.setParameter("company", crcName);
		Set<CarType> types = (Set<CarType>)query.getSingleResult();
		em.close();
		for (CarType ct : types) {
			names.add(ct.getName());
		}
		return names;
	}

	/**
	 * Get all registered car rental companies
	 *
	 * @return	the list of car rental companies
	 */
	public Collection<String> getAllRentalCompanyNames() {
		Set<String> names = new HashSet<String>();
		EntityManager em = EMF.get().createEntityManager();
		Query query = em.createQuery("SELECT crc.name FROM CarRentalCompany crc");
		names.addAll(query.getResultList());
		em.close();
		return names;
	}

	/**
	 * Create a quote according to the given reservation constraints (tentative reservation).
	 * 
	 * @param	company
	 * 			name of the car renter company
	 * @param	renterName 
	 * 			name of the car renter 
	 * @param 	constraints
	 * 			reservation constraints for the quote
	 * @return	The newly created quote.
	 *  
	 * @throws ReservationException
	 * 			No car available that fits the given constraints.
	 */
	public Quote createQuote(String company, String renterName, ReservationConstraints constraints) throws ReservationException {
		EntityManager em = EMF.get().createEntityManager();
		CarRentalCompany crc = em.find(CarRentalCompany.class, company);
		Quote out = null;

		if (crc != null) {
			out = crc.createQuote(constraints, renterName);
		} else {
			throw new ReservationException("CarRentalCompany not found.");
		}
		em.persist(crc);
		em.close();
		return out;
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
	public Reservation confirmQuote(Quote q) throws ReservationException{
		EntityManager em = EMF.get().createEntityManager();
    	CarRentalCompany crc = em.find(CarRentalCompany.class, q.getRentalCompany());
        Reservation r = crc.confirmQuote(q);
        em.persist(r);
        em.close();
        return r;
	}

	/**
	 * Confirm the given list of quotes
	 * 
	 * @param 	quotes 
	 * 			the quotes to confirm
	 * 
	 * @throws 	ReservationException
	 * 			One of the quotes cannot be cList<Quote> quotesonfirmed. 
	 * 			Therefore none of the given quotes is confirmed.
	 */
	public void confirmQuotes(List<Quote> quotes) throws ReservationException { 
		failedReservationsPerUser.put(quotes.get(0).getCarRenter(), null);
		
		Queue queue = QueueFactory.getDefaultQueue();
		TaskOptions task = withUrl("/worker").param("amountOfQuotes", quotes.size()+"");
		
		int index = 0;
		for(Quote q : quotes){
			task = task.param("carRenter"+index, q.getCarRenter()).param("carType"+index, q.getCarType())
					.param("company"+index, q.getRentalCompany()).param("price"+index, q.getRentalPrice()+"")
					.param("startDate"+index, q.getStartDate().toString()).param("endDate"+index, q.getEndDate().toString());			
		index++;
		}
		queue.add(task);
	}

	/**
	 * Get all reservations made by the given car renter.
	 *
	 * @param 	renter
	 * 			name of the car renter
	 * @return	the list of reservations of the given car renter
	 */
	public List<Reservation> getReservations(String renter) {
		List<Reservation> out = new ArrayList<Reservation>();
		EntityManager em = EMF.get().createEntityManager();
		Query q = em.createQuery(
				"SELECT r FROM Reservation r WHERE r.carRenter=:renter")
				.setParameter("renter", renter);
		out.addAll(q.getResultList());
		return out;
	}

	/**
	 * Get the car types available in the given car rental company.
	 *
	 * @param 	crcName
	 * 			the given car rental company
	 * @return	The list of car types in the given car rental company.
	 */
	@SuppressWarnings("unchecked")
	public Collection<CarType> getCarTypesOfCarRentalCompany(String crcName) {
		EntityManager em = EMF.get().createEntityManager();
		Collection<CarType> types = (Collection<CarType>) em.createQuery(
				"SELECT crc.carTypes FROM CarRentalCompany crc WHERE crc.name=:company")
				.setParameter("company", crcName).getSingleResult();
		em.close();
		return types;
	}

	/**
	 * Get the list of cars of the given car type in the given car rental company.
	 *
	 * @param	crcName
	 * 			name of the car rental company
	 * @param 	carType
	 * 			the given car type
	 * @return	A list of car IDs of cars with the given car type.
	 */
	public Collection<Integer> getCarIdsByCarType(String crcName, CarType carType) {
		Collection<Integer> out = new ArrayList<Integer>();
		for (Car c : getCarsByCarType(crcName, carType)) {
			out.add(c.getId());
		}
		return out;
	}

	/**
	 * Get the amount of cars of the given car type in the given car rental company.
	 *
	 * @param	crcName
	 * 			name of the car rental company
	 * @param 	carType
	 * 			the given car type
	 * @return	A number, representing the amount of cars of the given car type.
	 */
	public int getAmountOfCarsByCarType(String crcName, CarType carType) {
		return this.getCarsByCarType(crcName, carType).size();
	}

	/**
	 * Get the list of cars of the given car type in the given car rental company.
	 *
	 * @param	crcName
	 * 			name of the car rental company
	 * @param 	carType
	 * 			the given car type
	 * @return	List of cars of the given car type
	 */
	private List<Car> getCarsByCarType(String crcName, CarType carType) {
		EntityManager em = EMF.get().createEntityManager();
		List<Car> cars = new ArrayList<Car>();
		CarRentalCompany crc = em.find(CarRentalCompany.class, crcName);
		for (CarType ct : crc.getAllCarTypes()) {
			if (ct.equals(carType)) {
				cars.addAll(ct.getCars());
			}
		}
		em.close();
		return cars;
	}

	/**
	 * Check whether the given car renter has reservations.
	 *
	 * @param 	renter
	 * 			the car renter
	 * @return	True if the number of reservations of the given car renter is higher than 0.
	 * 			False otherwise.
	 */
	public boolean hasReservations(String renter) {
		return this.getReservations(renter).size() > 0;		
	}	
	
	public static ReservationException checkUserLastConfirm(String user){
		ReservationException re = failedReservationsPerUser.get(user);
		return re;
	}
}