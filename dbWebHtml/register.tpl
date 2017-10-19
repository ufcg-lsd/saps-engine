<#include "./header.tpl">

      <div class="row notLogged">
      	<div class="col-lg-4 col-lg-offset-4">
      		<form class="form-signin form-register" id="form-register">
		        <h2 class="form-signin-heading">Register</h2>
		        <label for="inputName" class="sr-only">Name</label>
		        <input type="text" id="inputName" class="form-control" placeholder="Name" required autofocus>
		        <label for="inputEmail" class="sr-only">Email address</label>
		        <input type="email" id="inputEmail" class="form-control" placeholder="Email address" required>
		        <label for="inputPassword" class="sr-only">Password</label>
		        <input type="password" id="inputPassword" class="form-control" placeholder="Password" required>
		        <label for="inputPasswordConfirmation" class="sr-only">Password confirmation</label>
		        <input type="password" id="inputPasswordConfirmation" class="form-control" placeholder="Password confirmation" required>
		        <div class="checkbox">
		          <label>
		            <input type="checkbox" id="inputNotify" value="notify-me"> Notify me after processing images.
		          </label>
		        </div>
		        <button class="btn btn-lg btn-primary btn-block" type="submit">Register</button>
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
