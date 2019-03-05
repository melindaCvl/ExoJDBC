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
	}
	
	/**
	 * D�sactive les utilisateurs de plus de 10 ans
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
		// on r�cup�re les droits
		ArrayList<Integer> listeDroits = retournerDroits(droit);

		// on d�sactive l'auto commit
		try {
			connection.setAutoCommit(false);
		} catch (Exception e) {}

		// on cr�� l'utilisateur
		String requete = "insert into Utilisateur (login, dateInscription, actif) values (?, ?, ?)";
		
		try (PreparedStatement pstmt = connection.prepareStatement(requete, Statement.RETURN_GENERATED_KEYS)) {
			pstmt.setString(1, u.getLogin());
			pstmt.setDate(2, u.getInscription());
			pstmt.setBoolean(3, u.isActif());
			
			// Ajout de l'utilisateur
			pstmt.executeUpdate();
			
			// on v�rifie que tous les droits existent tous
			// si un droit n'existe pas, on ne valide pas la transaction et le programme s'arr�te
			int nbDroitsExistants = listeDroits.size();
			int nbDroitsVoulus = droit.length;
			
			if(nbDroitsExistants != nbDroitsVoulus) {
				connection.rollback();
				throw new DroitInconnuException();
			}

			// on ins�re les droits
			try (ResultSet resultSet = pstmt.getGeneratedKeys()) {
				if (resultSet.next()) {
					int key = resultSet.getInt(1);

					//boucle for permettant d'ins�rer des lignes dans table_utilisateurs
					for (int unDroit : listeDroits) {
						addDroitUtilisateur(key, unDroit);
					}
				}
			}
			
			connection.commit();
			
		} catch (Exception e) {}
	}
	
	/**
	 * Retourne les id correspondants aux libell�s des droits pass�s en param�tres
	 * @param droit
	 * @return
	 */
	private ArrayList<Integer> retournerDroits(String... droit) {
		ArrayList<Integer> listeDroit = new ArrayList<>();

		try {
			connection.setAutoCommit(true);
		} catch (Exception e) {
			
		}
		
		// on cr�� la requ�te param�tr�e en fonction du nombre de droits dans le tableau pass� en param�tre
		String requete = "SELECT * FROM Droit ";
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
		
		// on ins�re les valeurs dans la requ�te param�tr�e
		// on l'�xecute et stocke ses valeurs dans un arraylist
		try (PreparedStatement pstmt = connection.prepareStatement(requete)) {
			for (int indDroit = 0; indDroit < nbDroits; indDroit++) {
				int numDroit = indDroit + 1;
				pstmt.setString(numDroit, droit[indDroit]);
			}
			ResultSet rs = pstmt.executeQuery();
			
			while(rs.next()) {
				listeDroit.add(rs.getInt("id"));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return listeDroit;
	}
	
	/**
	 * Associe un utilisateur � un droit
	 * @param idUtilisateur
	 * @param droit
	 * @throws DroitInconnuException
	 */
	private void addDroitUtilisateur(int idUtilisateur, int droit) throws DroitInconnuException {
		String requete = "insert into Utilisateur_droit (id_utilisateur, id_droit) values (?, ?)";
		
		try (PreparedStatement pstmt = connection.prepareStatement(requete, Statement.RETURN_GENERATED_KEYS)) {
			pstmt.setInt(1, idUtilisateur);
			pstmt.setInt(2, droit);
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

		  // on parcourt l'ensemble des r�sultats retourn�s par la requ�te
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
	 * D�termine si un utilisateur est associ� � un droit
	 * @param login
	 * @param droit
	 * @return
	 * @throws SQLException
	 */
	public boolean isAutorise(String login, String droit) throws SQLException {
		boolean estAutorise = false;
		
		String requete = "SELECT * FROM Utilisateur_Droit INNER JOIN Utilisateur ON Utilisateur_Droit.id_utilisateur = Utilisateur.id WHERE id_utilisateur = ? AND id_droit = ? AND actif = true";

		try (java.sql.PreparedStatement pstmt = connection.prepareStatement(requete)) {

		  pstmt.setString(1, login);
		  pstmt.setString(2, droit);

		  ResultSet rs = pstmt.executeQuery();
		  
		  estAutorise = rs.next();
		}
		
		return estAutorise;
	}
}
