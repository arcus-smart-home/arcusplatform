#extends spy_tools content_spy
<div class="btn-group pt-2 pb-3" role="group">
	<a href="/spy?p=zip" class="btn btn-outline-primary" role="button">Summary</a>
	<a href="/spy?p=zipdb"  class="btn btn-outline-primary" role="button">Database</a>
	<button type="button" class="btn btn-primary" role="button">Messages</button>
	<a href="/spy?p=ziptools" class="btn btn-outline-primary" role="button">Tools</a>
</div>

<div class="row">
   <div class="col-2">
      <a href="/spy/dump?p=zipserialdump" class="btn btn-outline-primary" role="button" target="_blank">Serial Log</a>
   </div>
</div>

<div class="row">
	<div class="col">
	    <div class="card">
			<div class="card-header">
				<span class="lead">Messages</span>
			</div> <!-- card-header -->
			
			<div class="card-body">
				<div id="nodemsg">
				  <pre class="green-console px-2">
{{ zipmsgs }}
                  </pre>
				</div>
		    </div> <!-- card-body -->
	   </div> <!-- card -->
    </div> <!-- col -->
</div> <!-- row -->

<script>
  new Vue({
    el: '#nodemsg',
    data: {
      zipmsgs: ""
    },
    methods: {
      loadData() {
        this.$http.get('/spy/api?p=zipmsgapi').then(response => {
          this.zipmsgs = response.data.data;
        });
      }
    },
    created: function () {
      this.loadData();
      
      setInterval(function() {
        this.loadData();
      }.bind(this), 2000);
    }  
  });
</script>
