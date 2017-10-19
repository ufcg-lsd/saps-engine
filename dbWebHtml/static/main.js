var sebalEngineApi = {
	doLogin: function(userEmail, userPassword) {
		if (userEmail !== 'undefined' && userEmail !== '' && userPassword !== 'undefined' && userPassword !== '') {
			$.ajax({
				type: 'GET',
				url: '/images',
				headers: {'userEmail': userEmail, 'userPass': userPassword},
				success: function(data) {
					window.sessionStorage.loggedUser = userEmail;
					window.sessionStorage.loggedPassword = userPassword;
					$('.onlyLogged').removeClass('hide');
					$('.notLogged').addClass('hide');
					if ($('.userEmailPlaceholder').length > 0) {
						$('.userEmailPlaceholder').html(window.sessionStorage.loggedUser);
					}
					$('.doLogout').click(function(evt) {
						evt.preventDefault();
						sebalEngineApi.doLogout();
					});
				},
				error: function (error) {
					window.sessionStorage.removeItem('loggedUser');
					window.sessionStorage.removeItem('loggedPassword');
					toastr.error('Error code: ' + error.status + ', message: ' + error.statusText, 'Error while trying to sign in.');
				}
			});
		} else {
			toastr.error('Email and password are mandatories.');
		}
	},
	doLogout: function() {
		window.sessionStorage.removeItem('loggedUser');
		window.sessionStorage.removeItem('loggedPassword');
		window.location.reload();
	},
	doRegister: function(name, email, password, passwordConfirmation, notify) {
		if (name !== 'undefined' && name !== '' && email !== 'undefined' && email !== '' 
			&& password !== 'undefined' && password !== '' 
			&& passwordConfirmation !== 'undefined' 
			&& passwordConfirmation !== '') {
			
			if (password === passwordConfirmation) {
				if (notify) {
					notify = 'yes';
				} else {
					notify = 'no';
				}
				$.ajax({
					type: 'POST',
					url: '/user/register',
					data: {'userName': name, 'userEmail': email, 'userPass': password, 'userPassConfirm': passwordConfirmation, 'userNotify': notify},
					success: function(data) {
						toastr.success('Your account is now created and in a pending state. Wait for the administrator approval.', 'User registered.');
						$('form input').val('');
					},
					error: function (error) {
						toastr.error('Error code: ' + error.status + ', message: ' + error.statusText, 'Error while trying to register a new user.');
					}
				});
			} else {
				toastr.error('Password and password confirmation does not match.');
			}
			
		} else {
			toastr.error('All fields are mandatories.');
		}
	},
	doAddJob: function(email, password, firstYear, lastYear, region, sebalVersion, sebalTag) {
		if (firstYear !== 'undefined' && firstYear !== '' 
			&& lastYear !== 'undefined' && lastYear !== '' 
			&& region !== 'undefined' && region !== '') {
			toastr.success('Your job was submitted. Wait for the processing be completed. ' 
					+ 'If you activated the notifications you will get an email when finished.', 'Job submitted.');
			$.ajax({
				type: 'POST',
				url: '/images',
				data: {'userPass': password, 'userEmail': email, 'firstYear': firstYear, 'lastYear': lastYear, 
					'region': region, 'sebalVersion': sebalVersion, 'sebalTag': sebalTag},
				success: function(data) {
					toastr.success('Your job was submitted. Wait for the processing be completed. ' 
							+ 'If you activated the notifications you will get an email when finished.', 'Job submitted.');
					$('form input').val('');
				},
				error: function (error) {
					toastr.error('Error code: ' + error.status + ', message: ' + error.statusText, 'Error while trying to submit a job.');
				}
			});
			
		} else {
			toastr.error('First year, last year and region are mandatories.');
		}
	},
	doGetImages: function(email, password, callback) {
		$.ajax({
			type: 'GET',
			url: '/images',
			headers: {'userEmail': email, 'userPass': password},
			success: function(data) {
				callback(data);
			},
			error: function (error) {
				toastr.error('Error code: ' + error.status + ', message: ' + error.statusText, 'Error while trying to get images.');
			}
		});
	}
}



$(document).ready(function(){
	var loggedUser = false;
	if (typeof(Storage) !== 'undefined') {
		if (window.sessionStorage.loggedUser && window.sessionStorage.loggedPassword) {
			loggedUser = true;
		}
	}
	
	if (loggedUser) {
		$('.onlyLogged').removeClass('hide');
		$('.notLogged').addClass('hide');
		if ($('.userEmailPlaceholder').length > 0) {
			$('.userEmailPlaceholder').html(window.sessionStorage.loggedUser);
		}
		$('.doLogout').click(function(evt) {
			evt.preventDefault();
			sebalEngineApi.doLogout();
		});
	} else {
		if (window.location.pathname !== '/' && window.location.pathname !== '/ui/createAccount') {
			window.location.href='/';
		}
		$('.onlyLogged').addClass('hide');
		$('.notLogged').removeClass('hide');
		if ($('#form-signin').length > 0) {
			$('#form-signin').submit(function(evt) {
				evt.preventDefault();
				var inputEmail = $(this).find('#inputEmail').val();
				var inputPassword = $(this).find('#inputPassword').val();
				sebalEngineApi.doLogin(inputEmail, inputPassword);
			});
		}
	}
	
	if ($('#form-register').length > 0) {
		$('#form-register').submit(function(evt) {
			evt.preventDefault();
			var inputName = $(this).find('#inputName').val();
			var inputEmail = $(this).find('#inputEmail').val();
			var inputPassword = $(this).find('#inputPassword').val();
			var inputPasswordConfirmation = $(this).find('#inputPasswordConfirmation').val();
			var inputNotify = $(this).find('#inputNotify').prop('checked');
			sebalEngineApi.doRegister(inputName, inputEmail, inputPassword, inputPasswordConfirmation, inputNotify);
		});
	}
	
	if ($('#form-addjob').length > 0) {
		$('#form-addjob').submit(function(evt) {
			evt.preventDefault();
			var inputFirstYear = $(this).find('#inputFirstYear').val();
			var inputLastYear = $(this).find('#inputLastYear').val();
			var inputRegion = $(this).find('#inputRegion').val();
			var inputSebalVersion = $(this).find('#inputSebalVersion').val();
			var inputSebalTag = $(this).find('#inputSebalTag').val();
			sebalEngineApi.doAddJob(window.sessionStorage.loggedUser, window.sessionStorage.loggedPassword, 
					inputFirstYear, inputLastYear, inputRegion, inputSebalVersion, inputSebalTag);
		});
	}
	
	if (window.location.pathname === '/ui/listImages') {
		sebalEngineApi.doGetImages(window.sessionStorage.loggedUser, window.sessionStorage.loggedPassword, function(data) {
			$(data).each(function(index, item) {
				var accordionItem = '<div class="panel panel-default">';
				accordionItem += '<div class="panel-heading" role="tab" id="heading' + index + '">';
				accordionItem += '<h4 class="panel-title">';
				accordionItem += '<a role="button" data-toggle="collapse" data-parent="#accordionImageList" ';
				accordionItem += 'href="#collapse' + index + '" aria-expanded="true" aria-controls="collapse' + index + '">';
				accordionItem += 'Image name: ' + item.name + ', state: ' + item.state;
				accordionItem += '</a></h4></div>';
				accordionItem += '<div id="collapse' + index + '" class="panel-collapse collapse" role="tabpanel" aria-labelledby="heading' + index + '">';
				accordionItem += '<div class="panel-body">';
				accordionItem += '<p><strong>Image name:</strong> ' + item.name + '</p>';
				accordionItem += '<p><strong>Download link:</strong> ' + item.downloadLink + '</p>';
				accordionItem += '<p><strong>State:</strong> ' + item.state + '</p>';
				accordionItem += '<p><strong>Federation member:</strong> ' + item.federationMember + '</p>';
				accordionItem += '<p><strong>Priority:</strong> ' + item.priority + '</p>';
				accordionItem += '<p><strong>Station ID:</strong> ' + item.stationId + '</p>';
				accordionItem += '<p><strong>SEBAL version:</strong> ' + item.sebalVersion + '</p>';
				accordionItem += '<p><strong>SEBAL tag:</strong> ' + item.sebalTag + '</p>';
				accordionItem += '<p><strong>Crawler version:</strong> ' + item.crawlerVersion + '</p>';
				accordionItem += '<p><strong>Fetcher version:</strong> ' + item.fetcherVersion + '</p>';
				accordionItem += '<p><strong>Blowout version:</strong> ' + item.blowoutVersion + '</p>';
				accordionItem += '<p><strong>F mask version:</strong> ' + item.fmaskVersion + '</p>';
				accordionItem += '<p><strong>Creation time:</strong> ' + item.creationTime + '</p>';
				accordionItem += '<p><strong>Update time:</strong> ' + item.updateTime + '</p>';
				accordionItem += '<p><strong>Status:</strong> ' + item.status + '</p>';
				accordionItem += '<p><strong>Error:</strong> ' + item.error + '</p>';
				accordionItem += '</div></div></div>';
				$('#accordionImageList').append(accordionItem);
			});
		});
	}
});