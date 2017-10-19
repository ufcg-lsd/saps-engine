<#include "./header.tpl">

      <!-- Jumbotron -->
      <div class="jumbotron hide onlyLogged">
        <h2>Hi <span class="userEmailPlaceholder">user</span>!</h2>
        <p class="lead">Welcome to SEBAL Engine UI.</p>
      </div>
      
      <div class="row hide notLogged">
      	<div class="col-lg-4 col-lg-offset-4">
      		<form class="form-signin" id="form-signin">
		        <h2 class="form-signin-heading">Please sign in</h2>
		        <label for="inputEmail" class="sr-only">Email address</label>
		        <input type="email" id="inputEmail" class="form-control" placeholder="Email address" required autofocus>
		        <label for="inputPassword" class="sr-only">Password</label>
		        <input type="password" id="inputPassword" class="form-control" placeholder="Password" required>
		        <button class="btn btn-lg btn-primary btn-block" type="submit">Sign in</button>
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
