package com.rapidftr.repository;

import com.rapidftr.CustomTestRunner;
import com.rapidftr.database.DatabaseSession;
import com.rapidftr.database.ShadowSQLiteHelper;
import com.rapidftr.model.Child;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static com.rapidftr.model.Child.History.*;
import static com.rapidftr.utils.JSONMatcher.equalJSONIgnoreOrder;
import static com.rapidftr.utils.JSONMatcher.hasJSONObjects;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.*;

@RunWith(CustomTestRunner.class)
public class ChildRepositoryTest {

    public DatabaseSession session;
    public ChildRepository repository;

    @Before
    public void setupSession() {
        session = new ShadowSQLiteHelper().getSession();
        repository = new ChildRepository("user1", session);
    }

    @Test
    public void shouldCreateChildRecord() throws JSONException {
        repository.createOrUpdate(new Child("id1", "user1", null));
        assertThat(repository.size(), equalTo(1));
    }

    @Test
    public void shouldUpdateChildRecordIfIdAlreadyExists() throws Exception {
        ChildRepository repository = new ChildRepository("user1", session);
        repository.createOrUpdate(new Child("id1", "user1", "{ 'test1' : 'value1', 'test2' : 0, 'test3' : [ '1', 2, '3' ] }"));
        String updateString = "{ 'test1' : 'value1' }";
        String expectedString = "{'created_by':'user1','test1':'value1','unique_identifier':'id1'}";
        repository.createOrUpdate(new Child("id1", "user1", updateString));
        Child child = repository.get("id1");
        assertThat(child.getUniqueId(), equalTo("id1"));
        assertThat(child.values(), equalJSONIgnoreOrder(expectedString));
    }

    @Test
    public void shouldGetCorrectlyDeserializesData() throws JSONException, IOException {
        Child child1 = new Child("id1", "user1", "{ 'test1' : 'value1', 'test2' : 0, 'test3' : [ '1', 2, '3' ] }");
        repository.createOrUpdate(child1);

        Child child2 = repository.get("id1");
        assertThat(child1.values(), equalJSONIgnoreOrder(child2.values()));
    }

    @Test
    public void shouldCorrectlyGetSyncedState() throws JSONException, IOException {
        Child syncedChild = new Child("syncedID", "user1", null, true);
        Child unsyncedChild = new Child("unsyncedID", "user1", null, false);
        repository.createOrUpdate(syncedChild);
        repository.createOrUpdate(unsyncedChild);

        assertThat(repository.get("syncedID").isSynced(), is(true));
        assertThat(repository.get("unsyncedID").isSynced(), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void getShouldThrowExceptionIfRecordDoesNotExist() throws JSONException {
        repository.get("blah");
    }

    @Test
    public void shouldReturnMatchedChildRecords() throws JSONException, IOException {
        Child child1 = new Child("id1", "user1", "{ 'name' : 'child1', 'test2' : 0, 'test3' : [ '1', 2, '3' ] }");
        Child child2 = new Child("id2", "user2", "{ 'name' : 'child2', 'test2' : 0, 'test3' : [ '1', 2, '3' ] }");
        Child child3 = new Child("id3", "user3", "{ 'name' : 'child3', 'test2' :  'child01', 'test3' : [ '1', 2, '3' ] }");
        Child child4 = new Child("child1", "user4", "{ 'name' : 'child4', 'test2' :  'test2', 'test3' : [ '1', 2, '3' ] }");
        repository.createOrUpdate(child1);
        repository.createOrUpdate(child2);
        repository.createOrUpdate(child3);
        repository.createOrUpdate(child4);

        List<Child> children = repository.getAllChildren();
        assertEquals(4, children.size());
    }


    @Test
    public void shouldCorrectlyGetSyncedStateWhenGettingAllRecords() throws JSONException, IOException {
        Child syncedChild = new Child("syncedID", "user1", null, true);
        Child unsyncedChild = new Child("unsyncedID", "user1", null, false);
        repository.createOrUpdate(syncedChild);
        repository.createOrUpdate(unsyncedChild);

        List<Child> all = repository.getChildrenByOwner();
        assertThat(all.get(0).isSynced(), is(true));
        assertThat(all.get(1).isSynced(), is(false));
    }

    @Test @Ignore // This expectation is no longer true, All users will be able to see all records
    public void shouldOnlyReturnsOwnRecords() throws JSONException {
        repository.createOrUpdate(new Child("id1", "user1", null));

        ChildRepository anotherUsersRepository = new ChildRepository("user2", session);
        anotherUsersRepository.createOrUpdate(new Child("id2", "user2", null));

        assertThat(repository.exists("id2"), is(false));
        assertThat(anotherUsersRepository.exists("id1"), is(false));
    }

    @Test
    public void shouldReturnsAllRecords() throws JSONException, IOException {
        Child child1 = new Child("id1", "user1", null);
        Child child2 = new Child("id2", "user1", null);
        repository.createOrUpdate(child1);
        repository.createOrUpdate(child2);

        List<Child> children = repository.getChildrenByOwner();
        assertThat(children.size(), equalTo(2));
        assertThat(children, hasJSONObjects(child1, child2));
    }

    @Test
    public void shouldOnlyReturnsOwnRecordsWhenGettingAll() throws JSONException {
        Child child1 = new Child("id1", "user1", null);
        repository.createOrUpdate(child1);

        ChildRepository anotherUsersRepository = new ChildRepository("user2", session);
        Child child2 = new Child("id2", "user2", null);
        anotherUsersRepository.createOrUpdate(child2);

        assertThat(repository.getChildrenByOwner(), not(hasItem(child2)));
        assertThat(anotherUsersRepository.getChildrenByOwner(), not(hasItem(child1)));
    }

    @Test
    public void shouldReturnAllUnSyncedRecords() throws JSONException {
        Child child1 = new Child("id1", "user1", null);
        Child child2 = new Child("id2", "user1", null, true);
        repository.createOrUpdate(child1);
        repository.createOrUpdate(child2);

        List<Child> children = repository.toBeSynced();
        assertThat(children.size(), equalTo(1));
        assertThat(children, hasItems(child1));
    }

    @Test(expected = JSONException.class)
    public void shouldRaiseRuntimeExceptionIfTheRequiredChildPropertiesAreNotPopulated() throws RuntimeException, JSONException {
        Child child = new Child();
        assertThat(child.isSynced(), is(false));
        repository.createOrUpdate(child);
    }

    @Test
    public void shouldReturnTrueWhenAChildWithTheGivenIdExistsInTheDatabase() {
        assertThat(repository.exists("1234"), is(false));
    }

    @Test
    public void shouldReturnFalseWhenAChildWithTheGivenIdDoesNotExistInTheDatabase() throws JSONException {
        Child child1 = new Child("iAmARealChild", "user1", null);

        repository.createOrUpdate(child1);

        assertThat(repository.exists("iAmARealChild"), is(true));
    }

    @Test
    public void shouldUpdateAnExstingChild() throws JSONException {
        Child child = new Child("id1", "user1", "{ 'test1' : 'value1', 'test2' : 0, 'test3' : [ '1', 2, '3' ] }");
        repository.createOrUpdate(child);
        child.put("someNewField", "someNewValue");

        repository.update(child);
        Child updatedChild = repository.get("id1");

        assertThat(updatedChild.get("someNewField").toString(), is("someNewValue"));
    }

    @Test
    public void shouldAddHistoriesIfChildHasBeenUpdated() throws JSONException {
        Child existingChild = new Child("id", "user1", "{'name' : 'old-name'}");
        repository.createOrUpdate(existingChild);

        Child updatedChild = new Child("id", "user1", "{'name' : 'updated-name'}");
        Child spyUpdatedChild = spy(updatedChild);
        List<Child.History> histories = new ArrayList<Child.History>();
        Child.History history = updatedChild.new History();
        HashMap changes = new HashMap();
        HashMap fromTo = new LinkedHashMap();
        fromTo.put(FROM, "old-name");
        fromTo.put(TO, "new-name");
        changes.put("name", fromTo);
        history.put(USER_NAME, "user");
        history.put(DATETIME, "timestamp");
        history.put(CHANGES, changes);
        histories.add(history);

        doReturn(histories).when(spyUpdatedChild).changeLogs(existingChild);
        repository.createOrUpdate(spyUpdatedChild);

        verify(spyUpdatedChild).put(HISTORIES, "[{\"user_name\":\"user\",\"datetime\":\"timestamp\",\"changes\":{\"name\":{\"from\":\"old-name\",\"to\":\"new-name\"}}}]");
        Child savedChild = repository.get(updatedChild.getUniqueId());
        assertThat(savedChild.get(HISTORIES).toString(), is("[{\"user_name\":\"user\",\"datetime\":\"timestamp\",\"changes\":{\"name\":{\"to\":\"new-name\",\"from\":\"old-name\"}}}]"));
    }

    @Test
    public void shouldAppendHistoryIfHistoriesAlreadyExist() throws JSONException {
        Child existingChild = new Child("id","user1","{\"name\":\"old-name\",\"histories\":[{\"changes\":{\"name\":{}}}, {\"changes\":{\"sex\":{}}}]}");
        repository.createOrUpdate(existingChild);

        Child updatedChild = new Child("id", "user1", "{'name' : 'updated-name'}");
        Child spyUpdatedChild = spy(updatedChild);
        List<Child.History> histories = new ArrayList<Child.History>();
        Child.History history = updatedChild.new History();
        HashMap changes = new HashMap();
        HashMap fromTo = new LinkedHashMap();
        fromTo.put(FROM, "old-name");
        fromTo.put(TO, "new-name");
        changes.put("name", fromTo);
        history.put(USER_NAME, "user");
        history.put(DATETIME, "timestamp");
        history.put(CHANGES, changes);
        histories.add(history);

        doReturn(histories).when(spyUpdatedChild).changeLogs(existingChild);
        repository.createOrUpdate(spyUpdatedChild);

        verify(spyUpdatedChild).put(HISTORIES, "[{\"changes\":{\"name\":{}}},{\"changes\":{\"sex\":{}}},{\"user_name\":\"user\",\"datetime\":\"timestamp\",\"changes\":{\"name\":{\"from\":\"old-name\",\"to\":\"new-name\"}}}]");
    }

    @Test
    public void shouldConstructTheHistoryObjectIfHistoriesArePassedAsStringInContent() throws JSONException {
        Child child = new Child("id", "user1", "{\"histories\":[{\"changes\":{\"name\":{\"from\":\"old-name\",\"to\":\"new-name\"}}}, {\"changes\":{\"sex\":{\"from\":\"\",\"to\":\"male\"}}}]}", false);
        repository.createOrUpdate(child);
        List<Child> children = repository.toBeSynced();
        JSONArray histories = (JSONArray) children.get(0).get(HISTORIES);
        assertThat(histories.length(), is(2));
        JSONObject name = (JSONObject) ((JSONObject) ((JSONObject) histories.get(0)).get("changes")).get("name");
        JSONObject sex = (JSONObject) ((JSONObject) ((JSONObject) histories.get(1)).get("changes")).get("sex");
        assertThat(name.get("from").toString(), is("old-name"));
        assertThat(name.get("to").toString(), is("new-name"));
        assertThat(sex.get("from").toString(), is(""));
        assertThat(sex.get("to").toString(), is("male"));
    }
}
