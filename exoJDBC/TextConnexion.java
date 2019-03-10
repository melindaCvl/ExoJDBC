package exoJDBC;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class TextConnexion {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/droitAcces?useSSL=false", "root", "root")) {
			DroitAccesDao dao = new DroitAccesDao(connection);
			
			// Désactive les utilisateurs qui se sont inscrits il y a plus de 10 ans
			dao.desactiverAnciensUtilisateurs();
			
			// Retourne la liste des utilisateurs
			List<Utilisateur> liste = dao.getUtilisateurs();
			parcourirUser(liste);
			
			// Détermine si un utilisateur a un droit
			boolean autorise = dao.isAutorise("doe", "connexion");
			
			// Ajout d'un utilisateur avec des droits tous existants
			Utilisateur u = new Utilisateur(5, "jeSuisUnLogin", new java.sql.Date(System.currentTimeMillis()), true);
			String[] listeDroits = {"connexion", "ecriture"};
			
			try {
				dao.addUtilisateur(u, listeDroits);
			} catch (Exception e) {}
				
			// Ajout d'un utilisateur avec des droits dont un qui n'existe pas
			String[] listeDroits2 = {"connexion", "ecriture", "fromage"};		// le droit "fromage n'existe pas, rien ne doit être créé
			
			try {
				 dao.addUtilisateur(u, listeDroits2);
			} catch (Exception e) {}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Permet de parcourir la liste des utilisateurs
	 */
	private static void parcourirUser(List<Utilisateur> liste) {
		for(Utilisateur user : liste) {
			System.out.println(user.getId());
			System.out.println(user.getLogin());
			System.out.println(user.getInscription());
			System.out.println(user.isActif());
		}
	}
}
