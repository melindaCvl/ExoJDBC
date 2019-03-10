package exoJDBC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DroitAccesDao {
	private Connection connection;
	
	public DroitAccesDao (Connection connection) {
		this.connection = connection;
		
		// on désactive l'auto commit
		try {
			connection.setAutoCommit(false);
		} catch (Exception e) {}
	}
	
	/**
	 * Désactive les utilisateurs de plus de 10 ans
	 */
	public void desactiverAnciensUtilisateurs() throws SQLException {
		try (Statement stmt = connection.createStatement())	{
			String requete = "UPDATE Utilisateur SET actif = false WHERE YEAR(CURRENT_DATE) - YEAR(dateInscription)> 10;";
			stmt.executeUpdate(requete);
		}
	}
	
	/**
	 * Ajoute un utilisateur avec des droits
	 * N'ajoute rien si l'un des droits est inexistant
	 * @param u
	 * @param droit
	 * @throws DroitInconnuException
	 */
	public void addUtilisateur(Utilisateur u, String... droit) throws DroitInconnuException {
		// on créé l'utilisateur
		String requete = "insert into Utilisateur (login, dateInscription, actif) values (?, ?, ?)";
		
		try (PreparedStatement pstmt = connection.prepareStatement(requete, Statement.RETURN_GENERATED_KEYS)) {
			pstmt.setString(1, u.getLogin());
			pstmt.setString(2, u.getInscription() + "");
			pstmt.setBoolean(3, u.isActif());
			
			// Ajout de l'utilisateur
			pstmt.executeUpdate();

			// on insère les droits
			try (ResultSet resultSet = pstmt.getGeneratedKeys()) {
				if (resultSet.next()) {
					int key = resultSet.getInt(1);

					//boucle for permettant d'insérer des lignes dans table_utilisateurs
					for (String unDroit : droit) {
						addDroitUtilisateur(key, unDroit);
					}
				}
			}
			
			System.out.println("Succès de l'ajout de l'utilisateur");
			connection.commit();
			
		} catch (Exception e) {
			try {
				System.out.println("Echec de l'ajout de l'utilisateur");
				connection.rollback();
			} catch (Exception exc) {}
		}
	}
	
	/**
	 * Retourne les id correspondants aux libellés des droits passés en paramètres
	 * @param droit
	 * @return
	 */
	private ArrayList<Integer> retournerDroits(String... droit) {
		ArrayList<Integer> listeDroit = new ArrayList<>();
		
		// on créé la requête paramétrée en fonction du nombre de droits dans le tableau passé en paramètre
		String requete = "SELECT id FROM Droit ";
		int nbDroits = droit.length;
		
		for (int indDroit = 0; indDroit < nbDroits; indDroit++) {
			
			if (indDroit == 0) {
				requete = requete + "WHERE ";	
			}
			
			requete = requete + "libelle = ? ";
			
			if(indDroit < nbDroits - 1) {
				requete = requete + "OR ";
			}
		}
		
		// on insère les valeurs dans la requête paramétrée
		// on l'éxecute et stocke ses valeurs dans un arraylist
		try (PreparedStatement pstmt = connection.prepareStatement(requete)) {
			for (int indDroit = 0; indDroit < nbDroits; indDroit++) {
				int numDroit = indDroit + 1;
				pstmt.setString(numDroit, droit[indDroit]);
			}
			ResultSet rs = pstmt.executeQuery();
			
			connection.commit();
			
			while(rs.next()) {
				listeDroit.add(rs.getInt("id"));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return listeDroit;
	}
	
	/**
	 * Associe un utilisateur à un droit
	 * @param idUtilisateur
	 * @param droit
	 * @throws DroitInconnuException
	 */
	private void addDroitUtilisateur(int idUtilisateur, String droit) throws DroitInconnuException {
		String requete = "INSERT INTO Utilisateur_droit (id_utilisateur, id_droit) VALUES(?, (SELECT id FROM Droit WHERE libelle = ?))";
		
		try (PreparedStatement pstmt = connection.prepareStatement(requete)) {
			pstmt.setInt(1, idUtilisateur);
			pstmt.setString(2, droit);
			pstmt.executeUpdate();
			
		} catch (Exception e) {
			throw new DroitInconnuException();
		}
	}
	
	/**
	 * Retourne la liste des utilisateurs
	 * @return
	 * @throws SQLException
	 */
	public List<Utilisateur> getUtilisateurs() throws SQLException {
		List<Utilisateur> listeUser = new ArrayList<Utilisateur>();
		
		String requete = "SELECT * FROM Utilisateur;";

		try (java.sql.Statement stmt = connection.createStatement();
		     java.sql.ResultSet resultSet = stmt.executeQuery(requete);) {

		  // on parcourt l'ensemble des résultats retournés par la requête
		  while (resultSet.next()) {
			  Integer id = resultSet.getInt("id");
			  String login = resultSet.getString("login");
			  java.sql.Date inscription = resultSet.getDate("dateInscription");
			  boolean actif = resultSet.getBoolean("actif");
			  
			  Utilisateur unUtili = new Utilisateur(id, login, inscription, actif);
			  listeUser.add(unUtili);
		  }
		}
		
		return listeUser;
	}
	
	/**
	 * Détermine si un utilisateur est associé à un droit
	 * @param login
	 * @param droit
	 * @return
	 * @throws SQLException
	 */
	public boolean isAutorise(String login, String droit) throws SQLException {
		boolean estAutorise = false;
		
		String requete = "SELECT * FROM (Utilisateur_Droit INNER JOIN Utilisateur ON Utilisateur_Droit.id_utilisateur = Utilisateur.id) INNER JOIN Droit ON Utilisateur_Droit.id_droit = Droit.id WHERE login = ? AND libelle = ? AND actif = true";

		try (java.sql.PreparedStatement pstmt = connection.prepareStatement(requete)) {

		  pstmt.setString(1, login);
		  pstmt.setString(2, droit);

		  ResultSet rs = pstmt.executeQuery();
		  
		  estAutorise = rs.next();
		}
		
		return estAutorise;
	}
}
