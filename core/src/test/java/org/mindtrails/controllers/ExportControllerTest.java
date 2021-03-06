package org.mindtrails.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mindtrails.Application;
import org.mindtrails.MockClasses.TestQuestionnaire;
import org.mindtrails.MockClasses.TestQuestionnaireRepository;
import org.mindtrails.MockClasses.TestUndeleteable;
import org.mindtrails.MockClasses.TestUndeleteableRepository;
import org.mindtrails.controller.ExportController;
import org.mindtrails.controller.QuestionController;
import org.mindtrails.domain.DoNotDelete;
import org.mindtrails.domain.RestExceptions.NotDeleteableException;
import org.mindtrails.domain.questionnaire.LinkedQuestionnaireData;
import org.mindtrails.domain.questionnaire.SecureQuestionnaireData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by dan on 10/23/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@ActiveProfiles("test")
public class ExportControllerTest extends BaseControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(ExportControllerTest.class);

    @Autowired
    private ExportController exportController;
    @Autowired
    private QuestionController questionController;
    @Autowired
    private TestQuestionnaireRepository repo;
    @Autowired
    private TestUndeleteableRepository repoU;

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    private void createTestEntry() {
        TestQuestionnaire q = new TestQuestionnaire();
        q.setDate(new Date());
        q.setValue("MyTestValue");
        repo.save(q);
        repo.flush();
    }

    @Override
    public Object[] getControllers() {
        return (new Object[]{exportController, questionController});
    }

    @Test
    public void testPostDataForm() throws Exception {
        ResultActions result = mockMvc.perform(post("/questions/TestQuestionnaire")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .with(SecurityMockMvcRequestPostProcessors.user(participant))
                .param("value", "cheese")
                .param("multiValue", "cheddar")
                .param("multiValue", "havarti"))
                .andExpect((status().is3xxRedirection()));
    }

    @Test
    public void testEntryDataIsReturned() {
        createTestEntry();
        List data = exportController.listData("TestQuestionnaire",0);
        assertThat((List<Object>)data, hasItem(hasProperty("value", is("MyTestValue"))));
    }

    @Test
    public void testEntryDataCanBeDeleted() {
        // There should be at least one entry.
        createTestEntry();
        SecureQuestionnaireData qd;

        List data =  exportController.listData("TestQuestionnaire",0);
        assertThat(data.size(), greaterThan(0));

        // Going to make the assumption this is QuestionnaireData
        for(Object o : data) {
            qd = (SecureQuestionnaireData)o;
            assertThat(qd.getId(), notNullValue());
            exportController.delete("TestQuestionnaire",qd.getId());
        }

        data =  exportController.listData("TestQuestionnaire",0);
        assertThat(data.size(), is(0));
    }

    @Test
    public void testSomeDataCannotBeDeleted() {
        // There should be at least one entry.
        TestUndeleteable u = new TestUndeleteable();
        u.setDate(new Date());
        u.setValue("MyTestValue");
        repoU.save(u);
        repoU.flush();

        LinkedQuestionnaireData qd;

        List data =  exportController.listData("TestUndeleteable",0);
        assertThat(data.size(), greaterThan(0));

        qd = (LinkedQuestionnaireData)data.get(0);
        assertThat(qd.getId(), notNullValue());

        assertTrue(TestUndeleteable.class.isAnnotationPresent(DoNotDelete.class));

        thrown.expect(NotDeleteableException.class);
        exportController.delete("TestUndeleteable",qd.getId());
    }

    @Test
    public void unknownFormsReturn404() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/export/ThisDoesNotExist")
                .with(user(admin)))
                .andExpect((status().is4xxClientError()))
                .andReturn();

    }

    /** Make sure that when the data is exported, the values are correctly encoded
     * @throws Exception
     */
    @Test
    public void testThatPostedDataIsExportedAsJSon() throws Exception {
        testPostDataForm();
        repo.flush();
        MvcResult result = mockMvc.perform(get("/api/export/TestQuestionnaire")
                .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect((status().is2xxSuccessful()))
                .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(result.getResponse().getContentAsString());
        assertEquals("Value is a top level object that contains the string 'cheese'", "cheese", actualObj.get(0).path("value").asText());
        System.out.println(actualObj);
    }

    @Test
    public void testMultiValueElementsCorrectlyPassedThrough() throws Exception {
        testPostDataForm();
        repo.flush();
        MvcResult result = mockMvc.perform(get("/api/export/TestQuestionnaire")
                .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect((status().is2xxSuccessful()))
                .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(result.getResponse().getContentAsString());
        Assert.assertThat(actualObj.findValue("multiValue").isArray(), is(true));
        Iterator<JsonNode> values = actualObj.findValue("multiValue").iterator();
        Assert.assertThat(values.next().textValue(), is("cheddar"));
        Assert.assertThat(values.next().textValue(), is("havarti"));
        System.out.println(actualObj);
    }


}
