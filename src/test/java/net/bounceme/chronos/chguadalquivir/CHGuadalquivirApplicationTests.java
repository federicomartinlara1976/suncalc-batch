package net.bounceme.chronos.chguadalquivir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.bounceme.chronos.suncalc.controller.JobController;
import net.bounceme.chronos.suncalc.model.Status;
import net.bounceme.chronos.suncalc.model.Task;

@SpringBootTest
@AutoConfigureMockMvc
public class CHGuadalquivirApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JobController controller;

	@MockBean
	private JobLauncher jobLauncher;

	private Status status;

	private JobExecution resultOK;

	private JobExecution resultFAIL;

	@BeforeEach
	public void setup() {
		status = Status.builder().version("1.8.0_202").platform("Linux").response("OK").build();
		resultOK = MetaDataInstanceFactory.createJobExecution();
		resultOK.setExitStatus(ExitStatus.COMPLETED);
		resultFAIL = MetaDataInstanceFactory.createJobExecution();
		resultFAIL.setExitStatus(ExitStatus.FAILED);
	}

	@AfterEach
	void tearDown() {
		status = null;
	}

	@Test
	void contextLoads() {
		assertThat(controller).isNotNull();
	}

	@Test
	public void testIndex() throws Exception {
		mockMvc.perform(get("/api/")).andDo(print()).andExpect(status().isOk())
				.andExpect(content().string(containsString("Hello world")));
	}

	@Test
	public void testStatus() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/status").contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(status))).andExpect(MockMvcResultMatchers.status().isOk())
				.andDo(MockMvcResultHandlers.print());
	}

	@Test
	public void testExecute() throws Exception {
		Mockito.doReturn(resultOK).when(jobLauncher).run(Mockito.isA(Job.class), Mockito.isA(JobParameters.class));

		Task task = new Task();
		task.setName("importJob");
		String json = asJsonString(task);

		RequestBuilder requestBuilder = createPostRequestBuilder(json);

		// Response OK
		MvcResult result = mockMvc.perform(requestBuilder).andReturn();
		MockHttpServletResponse response = result.getResponse();
		assertEquals(HttpStatus.OK.value(), response.getStatus());

		// Response 415
		task = new Task();
		json = asJsonString(task);
		requestBuilder = createPostRequestBuilder(json);
		result = mockMvc.perform(requestBuilder).andReturn();
		response = result.getResponse();
		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());

		// Response 500
		Mockito.doReturn(resultFAIL).when(jobLauncher).run(Mockito.isA(Job.class), Mockito.isA(JobParameters.class));

		task = new Task();
		task.setName("importJob");
		json = asJsonString(task);
		requestBuilder = createPostRequestBuilder(json);
		result = mockMvc.perform(requestBuilder).andReturn();
		response = result.getResponse();
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());

		// Task not found
		task = new Task();
		task.setName("task");
		json = asJsonString(task);
		requestBuilder = createPostRequestBuilder(json);
		result = mockMvc.perform(requestBuilder).andReturn();
		response = result.getResponse();
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
	}

	private RequestBuilder createPostRequestBuilder(String json) {
		RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/api/execute").accept(MediaType.APPLICATION_JSON)
				.content(json).contentType(MediaType.APPLICATION_JSON);
		return requestBuilder;
	}

	public static String asJsonString(final Object obj) {
		try {
			return new ObjectMapper().writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
