package com.board;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.jasper.tagplugins.jstl.core.Param;

import com.join.CustomInfo;
import com.util.DBConn;
import com.util.MyUtil;

public class BoardServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}
	
	protected void forward(HttpServletRequest req, HttpServletResponse resp, String url) throws ServletException, IOException {
		RequestDispatcher rd = 
				req.getRequestDispatcher(url);
		
		rd.forward(req, resp);
	}
	
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		req.setCharacterEncoding("UTF-8");
		String cp = req.getContextPath();
		
		Connection conn = DBConn.getConnection();
		BoardDAO dao = new BoardDAO(conn);
		
		MyUtil myUtil = new MyUtil();
		
		String uri = req.getRequestURI();
		
		String url;
		
		if(uri.indexOf("created.do") != -1) { // 전체 주소에서 created.do가 있나?
			
			HttpSession session = req.getSession();
			
			CustomInfo info = (CustomInfo)session.getAttribute("customInfo");
			// 클래스 파일에서는 이 방법으로 읽어온다
			
			if (info == null) {
				
				url = "/member/login.jsp";
				
				forward(req, resp, url);
				return;
				
			}
			
			url = "/boardTest/created.jsp"; 
			// 있으면 url에 진짜 주소를 저장해서 forward를 통해 주소가 바뀌지 않은 채 
			// 내부적으로만 created.jsp를 불러옴 (사용자에겐 주소창에 created.do가 보임)
			
			
			//포워딩으로 만들어주는 코드 (메소드 생성)
			
			forward(req, resp, url);
			
		} else if (uri.indexOf("created_ok.do") != -1) {
			
			BoardDTO dto = new BoardDTO();
			
			int maxNum = dao.getMaxNum();
			
			dto.setNum(maxNum + 1);
			dto.setSubject(req.getParameter("subject"));
			dto.setName(req.getParameter("name"));
			dto.setEmail(req.getParameter("email"));
			dto.setPwd(req.getParameter("pwd"));
			dto.setContent(req.getParameter("content"));
			dto.setIpAddr(req.getRemoteAddr());
			
			dao.insertData(dto);
			
			url = cp + "/bbs/list.do";
			resp.sendRedirect(url);
			
			
			
		} else if (uri.indexOf("list.do") != -1) {
			
			String pageNum = req.getParameter("pageNum");
			
			int currentPage = 1;
			
			if(pageNum != null) {
				
				currentPage = Integer.parseInt(pageNum);
				
			}
			
			String searchKey = req.getParameter("searchKey");
			String searchValue = req.getParameter("searchValue");
			
			if (searchValue == null) {
				
				searchKey = "subject";
				searchValue = "";
				
			} else {
				
				if(req.getMethod().equalsIgnoreCase("GET")) {
					searchValue = URLDecoder.decode(searchValue, "UTF-8");
				}
				
			}
			
			int dataCount = dao.getDataCount(searchKey, searchValue);
			
			int numPerPage = 5;
			int totalPage = myUtil.getPageCount(numPerPage, dataCount);
			
			if(currentPage > totalPage) {
				currentPage = totalPage;
			}
			
			int start = (currentPage - 1) * numPerPage + 1;
			int end = currentPage * numPerPage;
			
			List<BoardDTO> lists = 
					dao.getLists(start, end, searchKey, searchValue);
			
			String param = "";
			if(searchValue != null && !searchValue.equals("")) {
				param = "searchKey=" + searchKey;
				param += "&searchValue=" + URLEncoder.encode(searchValue,"UTF-8");
			}
			
			String listUrl = cp + "/bbs/list.do";
			
			if(!param.equals("")) { // 검색을 했으면 검색 결과를 list.do 뒤에 붙이기
				listUrl = listUrl + "?" + param;
			}
			
			String pageIndexList = 
					myUtil.pageIndexList(currentPage, totalPage, listUrl);
			
			String articleUrl = cp + "/bbs/article.do?pageNum=" + currentPage;
			// 글 쓰기 주소에 현재 페이지를 갖고 들어간다
			
			if(!param.equals("")) { // 만약 검색을 했으면 검색 값까지 Url에 갖고 들어간다
				
				articleUrl = articleUrl + "&" + param;
				
			}
			
			// 포워딩 할 데이터
			req.setAttribute("lists", lists);
			req.setAttribute("pageIndexList", pageIndexList);
			req.setAttribute("dataCount", dataCount);
			req.setAttribute("articleUrl", articleUrl);
			// list.jsp에 포워딩 될 데이터 4개
			
			url = "/boardTest/list.jsp"; // url에 실제 주소인 list.jsp를 저장하지만?
			
			forward(req, resp, url); // forward 방식으로 넘겨서 클라이언트에게는 보이지 않는다
			
		} else if (uri.indexOf("article.do") != -1) {
			
			int num = Integer.parseInt(req.getParameter("num"));
			String pageNum = req.getParameter("pageNum");
			
			String searchKey = req.getParameter("searchKey");
			String searchValue = req.getParameter("searchValue");
			
			if (searchValue != null) {
				searchValue = URLDecoder.decode(searchValue,"UTF-8");
			} else {
				searchKey = "subject";
				searchValue = "";
			}
			
			dao.updateHitCount(num);
			
			BoardDTO dto = dao.getReadData(num);
			
			if (dto == null) {
				url = cp + "/bbs/list.do";
				resp.sendRedirect(url); // 사용자에게 list.do를 보여준다
			}
			
			int lineSu = dto.getContent().split("\n").length;
			
			dto.setContent(dto.getContent().replaceAll("\n", "<br/>"));
			
			String param = "pageNum=" + pageNum;
			if(searchValue != null && !searchValue.equals("")) {
				param += "&searchKey=" + searchKey;
				param += "&searchValue=" + 
						URLEncoder.encode(searchValue,"UTF-8");
			
			}
			
			req.setAttribute("dto", dto);
			req.setAttribute("lineSu", lineSu);
			req.setAttribute("params", param);
			req.setAttribute("pageNum", pageNum);
			
			url = "/boardTest/article.jsp";
			
			forward(req, resp, url);
			
		} else if (uri.indexOf("updated.do") != -1) {
			
			int num = Integer.parseInt(req.getParameter("num"));
			String pageNum = req.getParameter("pageNum");
			
			String searchKey = req.getParameter("searchKey");
			String searchValue = req.getParameter("searchValue");
			
			if (searchValue != null) {
				searchValue = 
						URLDecoder.decode(searchValue,"UTF-8");
			}
			
			BoardDTO dto = dao.getReadData(num);
			
			if(dto == null) {
				url = cp + "/bbs/list.do";
				resp.sendRedirect(url);
			}
			
			String param = "pageNum=" + pageNum;
			
			if(searchValue != null && !searchValue.equals("")) {
				param += "&searchKey=" + searchKey;
				param += "&searchValue=" +
						URLEncoder.encode(searchValue,"UTF-8");
			}
			
			req.setAttribute("dto", dto);
			req.setAttribute("pageNum", pageNum);
			req.setAttribute("params", param);
			req.setAttribute("searchKey", searchKey);
			req.setAttribute("searchValue", searchValue);
			
			
			url = "/boardTest/updated.jsp";
			
			forward(req, resp, url);
			
			
		} else if (uri.indexOf("updated_ok.do") != -1) {
			
			String pageNum = req.getParameter("pageNum");
			String searchKey = req.getParameter("searchKey");
			String searchValue = req.getParameter("searchValue");
			
			BoardDTO dto = new BoardDTO();
			
			dto.setNum(Integer.parseInt(req.getParameter("num")));
			dto.setSubject(req.getParameter("subject"));
			dto.setName(req.getParameter("name"));
			dto.setEmail(req.getParameter("email"));
			dto.setPwd(req.getParameter("pwd"));
			dto.setContent(req.getParameter("content"));
			
			dao.updateData(dto);
			
			String param = "pageNum=" + pageNum;
			
			if(searchValue != null && !searchValue.equals("")) {
				param += "&searchKey=" + searchKey;
				param += "&searchValue=" +
						URLEncoder.encode(searchValue, "UTF-8");
			}
			
			url = cp + "/bbs/list.do?" + param;
			resp.sendRedirect(url);
			
			
			} else if (uri.indexOf("deleted_ok.do") != -1) {
			
			int num = Integer.parseInt(req.getParameter("num"));
			String pageNum = req.getParameter("pageNum");
			
			String searchKey = req.getParameter("searchKey");
			String searchValue = req.getParameter("searchValue");
			
			dao.deleteData(num);
			
			String param = "pageNum=" + pageNum;
			
			if(searchValue != null && !searchValue.equals("")) {
				param += "&searchKey=" + searchKey;
				param += "&searchValue=" +
						URLEncoder.encode(searchValue,"UTF-8");
			}
			
			url = cp + "/bbs/list.do?" + param;
			resp.sendRedirect(url);
			
		}
		
		
		
	}
	
}
