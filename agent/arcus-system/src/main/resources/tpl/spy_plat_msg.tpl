#extends spy_tools content_spy
<div class="mb-3">
  <span class="lead">Platform Messages</span>
</div>

<div id="messages" class="row">
  <div class="col">
  	<div class="card">
  	
  	  <div class="card-header">
  	  	<span class="lead">Incoming</span>
  	  </div>
  	  
  	  <div class="body-card">
  	    <div v-for="msg in incoming" class="mb-3 px-3">
  	    	{{ msg }}
  	    </div>
  	  </div>
  	
  	</div> <!-- card -->
  </div> <!-- col -->
  
  <div class="col">
  	<div class="card">
  	
  	  <div class="card-header">
  	  	<span class="lead">Outgoing</span>
  	  </div>
  	  
  	  <div class="body-card">
  	    <div v-for="msg in outgoing" class="mb-3 px-3">
  	    	{{ msg }}
  	    </div>
  	  </div>
  	
  	</div> <!-- card -->
  </div> <!-- col -->
</div> <!-- row -->

<script>
  new Vue({
    el: '#messages',
    data: {
      incoming: [],
      outgoing: []
    },
    methods: {
      loadData() {
        this.$http.get('/spy/api?p=getplatmsg').then(response => {
          this.incoming = response.data.incoming;
          this.outgoing = response.data.outgoing;
        });
      }
    },
    created: function () {
      console.log("Created Called!");
      this.loadData();
      
      setInterval(function() {
        this.loadData();
      }.bind(this), 3000);
    }  
  });
</script>