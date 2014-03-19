package edu.virginia.psyc.pi.DAO;

import edu.virginia.psyc.pi.Application;
import edu.virginia.psyc.pi.Trial;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: dan
 * Date: 3/18/14
 * Time: 6:58 AM
 * Assure that Trials are correctly stored and retrieved from the database.
 */
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class TrialDAOTest {

    @Autowired
    protected TrialDAOSql trialDAO;

    @Test
    @Transactional
    public void findTrials() {
        List<Trial> trials;

        Assert.assertTrue(true);
        trials = trialDAO.getTrials();
        Assert.assertNotNull(trials);
    }



}