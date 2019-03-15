#extends spy_tools content_spy
<div class="btn-group pt-2 pb-3" role="group">
	<a href="/spy?p=zip" class="btn btn-outline-primary" role="button">Summary</a>
	<a href="/spy?p=zipdb"  class="btn btn-outline-primary" role="button">Database</a>
	<a href="/spy?p=zipmsg" class="btn btn-outline-primary" role="button">Messages</a>
	<button type="button" class="btn btn-primary" role="button">Tools</button>
</div>

<div id="nodetools">
	<div class="row">
	   <div class="col-2">
	      <a href="#" class="btn btn-outline-primary" role="button" v-on:click="sendnif">Send Nif</a>
	   </div> <!-- col -->
	   <div class="col">
	      <div class="card">
	      	<div class="card-header">
	      		Learn Mode
	      	</div> <!-- card-header -->
	      	<div class="card-body">
	      		<div class="btn-group" role="group">
	      			<a href="#" class="btn btn-outline-primary" role="button" v-on:click="sendlearndisable">Disable</a>
	      			<a href="#" class="btn btn-outline-primary" role="button" v-on:click="sendlearnclassic">Classic (Direct)</a>
	      			<a href="#" class="btn btn-outline-primary" role="button" v-on:click="sendlearnnwi">NWI (Routable)</a>
	      		</div> <!-- btn-group -->
	      	</div> <!-- card-body -->
	      </div> <!-- card -->
	   </div> <!-- col -->
	</div> <!-- row -->
	
	<div class="row">
		<div class="col">
		    <div class="card">
				<div class="card-header">
					<span class="lead">Activity</span>
				</div> <!-- card-header -->
				
				<div class="card-body">
			       <pre class="green-console px-2">
{{ ziptoolsdump }}
	               </pre>
			    </div> <!-- card-body -->
		   </div> <!-- card -->
	    </div> <!-- col -->
	</div> <!-- row -->
</div>

<script>
  new Vue({
    el: '#nodetools',
    data: {
      ziptoolsdump: ""
    },
    methods: {
      loadData() {
        this.$http.get('/spy/api?p=ziptoolsapi').then(response => {
          this.ziptoolsdump = response.data.data;
        });
      },
      sendnif() {
      	this.$http.get('/spy/api?p=zipsendnif');
      },
      sendlearndisable() {
      	this.$http.get('/spy/api?p=ziplearndisable');
      },
      sendlearnclassic() {
      	this.$http.get('/spy/api?p=ziplearnclassic');
      },
      sendlearnnwi() {
      	this.$http.get('/spy/api?p=ziplearnnwi');
      }
    },
    created: function () {
      this.loadData();
      
      setInterval(function() {
        this.loadData();
      }.bind(this), 1000);
    }  
  });
</script>
