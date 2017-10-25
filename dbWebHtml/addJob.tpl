<#include "./header.tpl">

      <div class="row onlyLogged">
      	<div class="col-lg-6 col-lg-offset-3">
      		<form class="form-signin form-addjob" id="form-addjob">
		        <h2 class="form-signin-heading">Add Job</h2>
		        <label for="inputFirstYear" class="sr-only">First year*</label>
		        <input type="number" id="inputFirstYear" class="form-control" placeholder="First year. e.g.: 1984" required autofocus>
		        <label for="inputLastYear" class="sr-only">Last year*</label>
		        <input type="number" id="inputLastYear" class="form-control" placeholder="Last year. e.g.: 1984" required>
		        <label for="inputRegion" class="sr-only">Region*</label>
		        <input type="text" id="inputRegion" class="form-control" placeholder="Region" required>
		        <label for="inputSebalVersion" class="sr-only">SEBAL Version</label>
		        <input type="text" id="inputSebalVersion" class="form-control" placeholder="SEBAL Version. e.g.: https://github.com/ufcg-lsd/SEBAL.git">
		        <label for="inputSebalTag" class="sr-only">SEBAL Tag</label>
		        <input type="text" id="inputSebalTag" class="form-control" placeholder="SEBAL Tag">
		        <button class="btn btn-lg btn-primary btn-block" type="submit">Submit</button>
		      </form>
      	</div>
      </div>

      <!-- Site footer -->
      <footer class="footer">
        <p>&copy; 2016 Company, Inc.</p>
      </footer>
    </div> <!-- /container -->
  </body>
</html>
